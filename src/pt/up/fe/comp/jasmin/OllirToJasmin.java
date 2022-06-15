package pt.up.fe.comp.jasmin;

import com.javacc.parser.tree.Literal;
import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;


import static org.specs.comp.ollir.ElementType.*;

public class OllirToJasmin {

    int cond;

    private final ClassUnit classUnit;
    private ArrayList<String> imports;
    private String classname;

    public OllirToJasmin(ClassUnit classUnit){
        this.classUnit = classUnit;
        this.imports = classUnit.getImports();
        this.classname = "";
    }

    public String getClassCode(){

        var classCode = new StringBuilder();

        // Class Name
        this.classname = classUnit.getClassName();
        classCode.append(".class public ").append(this.classname).append("\n");

        // Super Class and Constructor
        classCode.append(".super ").append(getSuperClassName()).append("\n\n");

        for (Field field : classUnit.getFields()){
            classCode.append(getFieldCode(field)).append("\n");
        }

        classCode.append("\n");

        // Class Methods
        for (var method : classUnit.getMethods()){
            classCode.append(getMethodCode(method));
        }

        return classCode.toString();
    }


    private String getFieldCode(Field field){
        StringBuilder fieldCode = new StringBuilder();

        fieldCode.append(".field");

        AccessModifiers fieldAccessModifier = field.getFieldAccessModifier();
        if(fieldAccessModifier == AccessModifiers.DEFAULT){
            fieldCode.append(" private");
        }
        else{
            String accessModifierString = fieldAccessModifier.toString();
            fieldCode.append(" ").append(accessModifierString.toLowerCase());
        }

        // static field
        if (field.isStaticField()){
            fieldCode.append(" static");
        }
        // final field
        if (field.isFinalField()){
            fieldCode.append(" final");
        }

        fieldCode.append(" ").append(field.getFieldName());
        String fieldType = getJasminType(field.getFieldType());
        fieldCode.append(" ").append(fieldType);

        if(field.isInitialized()){
            fieldCode.append(" = ").append(field.getInitialValue());
        }

        return fieldCode.toString();
    }

    private String getFullyQualifiedName(String className){
        if (className == null){
            throw new RuntimeException("Null class has no super class");
        }
        for(var importString : this.imports){
            var splitImports = importString.split("\\.");

            String lastName;
            if(splitImports.length == 0){
                lastName = importString;
            }
            else{
                lastName = splitImports[splitImports.length-1];
            }

            if(lastName.equals((className))){
                return importString.replace('.', '/');
            }
        }
        return className;
    }

    private String getSuperClassName(){
        var superClass = classUnit.getSuperClass();
        if(superClass == null){
            return "java/lang/Object";
        }
        else{
            return getFullyQualifiedName(superClass);
        }
    }

    private String getMethodAccessModifier(Method method){
        AccessModifiers methodAccessModifier = method.getMethodAccessModifier();
        switch (methodAccessModifier){
            case PUBLIC:
                return "public";
            case PRIVATE:
                return "private";
            case PROTECTED:
                return "protected";
            case DEFAULT:
                return "public";
        }
        throw new RuntimeException("Could not get access modifier for method " + method.getMethodName());
    }

    private String getJasminElementType(ElementType elementType){
        switch (elementType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            default:
                throw new RuntimeException("Invalid type descriptor found");
        }
    }

    private String getJasminType(Type type){
        ElementType elementType = type.getTypeOfElement();
        StringBuilder typeDescriptor = new StringBuilder();

        switch (elementType){
            case ARRAYREF:
                // check type of array items
                ElementType childElemType = ((ArrayType) type).getTypeOfElements();
                typeDescriptor.append("[").append(getJasminElementType(childElemType));
                return typeDescriptor.toString();
            case OBJECTREF:
                String objectClass = ((ClassType) type).getName();
                for (String importedClass : imports){
                    if(importedClass.endsWith("." + objectClass)){
                        typeDescriptor.append("L");
                        typeDescriptor.append(importedClass.replace('.', '/')).append(";");
                        return typeDescriptor.toString();
                    }
                }
                throw new RuntimeException("AAAAAAAAAAAAAAAAAAAAAAAA");
            case CLASS:
                throw new NotImplementedException("Element type not implemented: " + elementType);
            default:
                typeDescriptor.append(getJasminElementType(elementType));
                return typeDescriptor.toString();

        }

        /*
            [X......... X[] (array of X)
            LY;........ class Y
            (X)Y....... X->Y (method with domain = X and range = Y)
        */
    }

    private String getMethodParameters(Method method){
        StringBuilder methodParametersString = new StringBuilder();

        ArrayList<Element> methodParameters = method.getParams();
        for (Element parameter : methodParameters){
            var parameterType = getJasminType(parameter.getType());
            methodParametersString.append(parameterType);
        }

        return methodParametersString.toString();
    }

    private String getMethodCode(Method method) {
        var methodCode = new StringBuilder();
        methodCode.append(getMethodHeader(method));
        methodCode.append(getMethodBody(method));
        methodCode.append(".end method\n\n");
        return methodCode.toString();
    }

    private String getMethodHeader(Method method){
        StringBuilder headerCode = new StringBuilder();

        headerCode.append(".method ").append(getMethodAccessModifier(method));

        // static method
        if (method.isStaticMethod()){
            headerCode.append(" static");
        }
        // final method
        if (method.isFinalMethod()){
            headerCode.append(" final");
        }

        // constructor method
        if (method.isConstructMethod()){
            headerCode.append(" <init>");
        }
        else{   // standard method
            headerCode.append(" ").append(method.getMethodName());
        }

        String methodParameters = getMethodParameters(method);
        headerCode.append("(").append(methodParameters).append(")");

        // Descriptor
        String returnTypeDescriptor = getJasminType(method.getReturnType());
        headerCode.append(returnTypeDescriptor).append("\n");

        return headerCode.toString();
    }

    private String getMethodBody(Method method){
        StringBuilder bodyCode = new StringBuilder();

        bodyCode.append("\t.limit stack 99\n");
        bodyCode.append("\t.limit locals 99\n");

        HashMap<String, Descriptor> varTable = method.getVarTable();

        for(Instruction inst : method.getInstructions()){
            bodyCode.append("\t").append(getInstructionCode(inst, varTable));
        }

        if (method.isConstructMethod()){
                bodyCode.append("\treturn\n");
        }

        return bodyCode.toString();
    }

    private String getInstructionCode(Instruction instruction, HashMap<String, Descriptor> varTable){
        /*
        FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
        instructionMap.put(CallInstruction.class, this::getCallInstructionCode);
        instructionMap.put(AssignInstruction.class, this::getAssignInstructionCode);
        instructionMap.put(ReturnInstruction.class, this::getReturnInstructionCode);
        instructionMap.put(SingleOpInstruction.class, this::getSingleOpInstructionCode);
        //instructionMap.put(SingleOpCondInstruction.class, this::getCode);
        instructionMap.put(BinaryOpInstruction.class, this::getBinaryOpInstructionCode);
        instructionMap.put(PutFieldInstruction.class, this::getPutFieldInstructionCode);
        instructionMap.put(GetFieldInstruction.class, this::getGetFieldInstructionCode);

        return instructionMap.apply(instruction);
        */
        InstructionType instructionType = instruction.getInstType();
        switch (instructionType){
            case CALL:
                return getCallInstructionCode( (CallInstruction) instruction, varTable);
            case ASSIGN:
                return getAssignInstructionCode( (AssignInstruction) instruction, varTable);
            case RETURN:
                return getReturnInstructionCode( (ReturnInstruction) instruction, varTable);
            case NOPER:
                return getSingleOpInstructionCode( (SingleOpInstruction) instruction, varTable);
                /*
            case SingleOpCondInstruction.class:
                return;
                */
            case BINARYOPER:
                return getBinaryOpInstructionCode( (BinaryOpInstruction) instruction, varTable);
            case PUTFIELD:
                return getPutFieldInstructionCode( (PutFieldInstruction) instruction, varTable);
            case GETFIELD:
                return getGetFieldInstructionCode( (GetFieldInstruction) instruction, varTable);
                /*
            case GOTO:
                return
                */
            case UNARYOPER:
                throw new NotImplementedException("UNARYOPER instruction type not implemented");
            default:
                throw new RuntimeException("Unknown instruction type " + instructionType);
        }
    }

    private String getCallInstructionCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){

        CallType methodInvocationType = instruction.getInvocationType();
        switch (methodInvocationType){
            case invokestatic:
                return getStaticInvocationCode(instruction, varTable);
            case invokevirtual:
                return getVirtualInvocationCode(instruction, varTable);
            case invokeinterface:
                throw new NotImplementedException("invokeinterface method invocation");
            case invokespecial:
                return getInvokeSpecialCode(instruction, varTable);
            case NEW:
                return getNewInvocationCode(instruction, varTable);
            case ldc:
                return getLDCInvocationCode(instruction, varTable);
            case arraylength:
                //simply load element into a register + array length
                return getArraylengthInvocationCode(instruction, varTable);
            default:
                throw new RuntimeException("Unknown method invocation type: " + methodInvocationType);
        }
    }

    private String getStaticInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        for (Element toLoad : instruction.getListOfOperands())
            instructionCode.append(loadElement(toLoad, varTable));

        instructionCode.append("\tinvokestatic ");

        String instructionCallerName = ((Operand) instruction.getFirstArg()).getName();
        String instructionName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        instructionCode.append(getFullyQualifiedName(instructionCallerName)).append("/").append(instructionName);

        instructionCode.append("(");
        for (Element operand : instruction.getListOfOperands()){
            instructionCode.append(getJasminType(operand.getType()));
        }
        instructionCode.append(")");

        instructionCode.append(getJasminType(instruction.getReturnType())).append("\n");

        return instructionCode.toString();
    }

    private String getVirtualInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        Element firstVirtualInstruction = instruction.getFirstArg();
        instructionCode.append(loadElement(firstVirtualInstruction, varTable));

        for (Element toLoad : instruction.getListOfOperands())
            instructionCode.append(loadElement(toLoad, varTable));

        instructionCode.append("\tinvokevirtual ");

        String instructionClassName = ((ClassType) instruction.getFirstArg().getType()).getName();
        String instructionName = ((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", "");
        instructionCode.append(getFullyQualifiedName(instructionClassName)).append("/").append(instructionName);

        instructionCode.append("(");
        for (Element operand : instruction.getListOfOperands()){
            instructionCode.append(getJasminType(operand.getType()));
        }
        instructionCode.append(")");

        instructionCode.append(getJasminType(instruction.getReturnType())).append("\n");


        return instructionCode.toString();
    }

    private String getInvokeSpecialCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        Element firstSpecialInstruction = instruction.getFirstArg();
        instructionCode.append(loadElement(firstSpecialInstruction, varTable));

        instructionCode.append("\tinvokespecial ");

        String instructionClassName = "";
        if(instruction.getFirstArg().getType().getTypeOfElement() == THIS){
            instructionClassName = getSuperClassName();
        }
        else{
            instructionClassName = this.classname;
        }

        instructionCode.append(instructionClassName).append("/<init>");

        instructionCode.append("(");
        for (Element operand : instruction.getListOfOperands()){
            instructionCode.append(getJasminType(operand.getType()));
        }
        instructionCode.append(")");

        instructionCode.append(getJasminType(instruction.getReturnType())).append("\n");

        return instructionCode.toString();
    }

    private String getNewInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        for (Element toLoad : instruction.getListOfOperands()) {
            instructionCode.append(loadElement(toLoad, varTable));
        }

        ElementType returnType = instruction.getReturnType().getTypeOfElement();
        switch (returnType){
            case ARRAYREF:
                ElementType arrayType = instruction.getListOfOperands().get(0).getType().getTypeOfElement();
                String arrayTypeString = "";
                switch (arrayType){
                    case INT32:
                        arrayTypeString = "int";
                        break;
                    case STRING:
                        arrayTypeString = "java/lang/String";
                        break;
                    default:
                        throw new RuntimeException("Uknown array type for new instruction " + arrayType);
                }
                instructionCode.append("newarray ").append(arrayTypeString).append("\n");
                break;
            case OBJECTREF:
                String instructionClassName = ((Operand) instruction.getFirstArg()).getName();
                instructionCode.append("new ").append(instructionClassName).append("\n");
                instructionCode.append("\tdup\n");  //push duplicate
                break;
            default:
                throw new RuntimeException("Unknown NEW invocation return type: " + returnType);
        }
        return instructionCode.toString();
    }

    private String getLDCInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        Element firstInstruction = instruction.getFirstArg();
        return loadElement(firstInstruction, varTable);
    }

    private String getArraylengthInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();
        Element firstInstruction = instruction.getFirstArg();
        instructionCode.append(loadElement(firstInstruction, varTable));
        instructionCode.append("\tarraylength\n");
        return instructionCode.toString();
    }

    private String getAssignInstructionCode(AssignInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        Operand opDest = (Operand) instruction.getDest();
        Descriptor opDescriptor = varTable.get(opDest.getName());
        int register = opDescriptor.getVirtualReg();
        ElementType opType = opDest.getType().getTypeOfElement();
        ElementType descriptorType = opDescriptor.getVarType().getTypeOfElement();



        // TODO: when right hand side expression is a binary operation ( a = x + y, for example)


        // Case Inc (Incrementation a += 1) (NOT WORKING | NEEDS FIX)
        /*
        if (instruction.getRhs().getInstType() == BINARYOPER){
            BinaryOpInstruction binOpInst = (BinaryOpInstruction) instruction.getRhs();
            if (binOpInst.getUnaryOperation().getOpType() == ADD){ // TODO: CONFIRM: Would just 'ADD' work???

                if (binOpInst.getRightOperand().isLiteral() && !binOpInst.getLeftOperand().isLiteral()){
                    Integer roIntValue = parseInt(((LiteralElement) binOpInst.getRightOperand()).getLiteral());
                    if (roIntValue == 1 && ((Operand) binOpInst.getLeftOperand()).getName().equals(opDest.getName()))
                        return "\tiinc " + register + " 1\n";
                }

                else 
                
                if (binOpInst.getRightOperand().isLiteral() && !binOpInst.getLeftOperand().isLiteral()){
                    Integer loIntValue = parseInt(((LiteralElement) binOpInst.getRightOperand()).getLiteral());
                    if (loIntValue == 1 && ((Operand) binOpInst.getRightOperand()).getName().equals(o.getName()))
                        return "\tiinc " + register + " 1\n";
                }
            }

        }
        */




        // Case 1
        if(descriptorType == ARRAYREF && opType != ARRAYREF){
            ArrayOperand arrayOperand = (ArrayOperand) opDest;
            Element index = arrayOperand.getIndexOperands().get(0);

            instructionCode.append(loadDescriptor(opDescriptor));
            instructionCode.append(loadElement(index, varTable));
        }

        String rhsCode = getInstructionCode(instruction.getRhs(), varTable);
        instructionCode.append(rhsCode);




        // Case 2
        if (opType == INT32 || opType == BOOLEAN){
            if(descriptorType == ARRAYREF){
                instructionCode.append("\t").append("iastore\n");
                return instructionCode.toString();
            }
            else{
                instructionCode.append("\t").append("istore");
            }
        }
        else {
            instructionCode.append("\t").append("astore");
        }

        if(register <= 3){
            instructionCode.append("_");
        }
        else{
            instructionCode.append(" ");
        }

        instructionCode.append(register).append("\n");

        return instructionCode.toString();
    }

    private String getReturnInstructionCode(ReturnInstruction instruction, HashMap<String, Descriptor> varTable){
        if(!instruction.hasReturnValue()){
            return "return\n";
        }

        StringBuilder instructionCode = new StringBuilder();
        instructionCode.append(loadElement(instruction.getOperand(), varTable));
        ElementType returnType = instruction.getReturnType().getTypeOfElement();
        switch (returnType){
            case INT32:
                instructionCode.append("\ti");
                break;
            case BOOLEAN:
                instructionCode.append("\ti");
                break;
            default:
                throw new NotImplementedException("Unknown NEW invocation return type: " + returnType);
        }
        instructionCode.append("return\n");

        return instructionCode.toString();
    }

    private String getSingleOpInstructionCode(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: CONFIRM
        return loadElement(instruction.getSingleOperand(), varTable);
        //throw new NotImplementedException("SingleOpInstruction");
    }






    // BINOP Dependency (OLD) !
    /*
    public String getUnaryOpInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();
        //instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
        instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
        instructionCode.append("\t");
        instructionCode.append(instruction.getUnaryOperation()); // Maybe missing getOp()
        instructionCode.append("\n");

        return instructionCode.toString();
    }
    // BINOP critical (OLD) !!
    private String getBinaryOpInstructionCode(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: CONFIRM this
        StringBuilder instructionCode = new StringBuilder();
        OperationType binOpInst = instruction.getUnaryOperation().getOpType();

        if (binOpInst == OperationType.NOTB){
                // Maybe Missing: Reset Stack size
                cond = cond + 1;

                String leftOpName = ((Operand) instruction.getLeftOperand()).getName();
                String rightOpName = ((Operand) instruction.getRightOperand()).getName();

                instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
                if (leftOpName.equals(rightOpName))
                    instructionCode.append("\tifeq");
                else{
                    instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
                    instructionCode.append("\t");
                    instructionCode.append(getComparison(instruction.getUnaryOperation()));
                }

                instructionCode.append(" True" + cond + "\n");
                instructionCode.append("\ticonst_0" + "\n");
                instructionCode.append("\tgoto Store" + cond + "\n");
                instructionCode.append("True" + cond + ":\n");
                instructionCode.append("\ticonst_t" + "\n");
                instructionCode.append("Store" + cond + ":\n");



                return instructionCode.toString();
            }


            
        else if(binOpInst == OperationType.ANDB){
                cond = cond + 1;
                // Maybe Missing: Reset Stack size

                instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
                instructionCode.append("\tifeq False" + cond + "\n");
                instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
                instructionCode.append("\tifeq False" + cond + "\n");
                instructionCode.append("\ticonst_1" + "\n");
                instructionCode.append("\tgoto Store" + cond + "\n");
                instructionCode.append("False" + cond + ":\n");
                instructionCode.append("\ticonst_0" + "\n");
                instructionCode.append("Store" + cond + ":\n");

                return instructionCode.toString();
            }

        else if(binOpInst == OperationType.LTH){
            cond = cond + 1;
            // Maybe Missing: Reset Stack size
            instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
            instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
            instructionCode.append("\t" + getComparison(instruction.getUnaryOperation()) + " True" + cond + "\n");
            instructionCode.append("\ticonst_0" + "\n");
            instructionCode.append("\tgoto Store" + cond + "\n");
            instructionCode.append("True" + cond + ":\n");
            instructionCode.append("\ticonst_1" + "\n");
            instructionCode.append("Store" + cond + ":\n");

            return instructionCode.toString();

        }

        else{ 
            instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
            instructionCode.append(UnaryOpInstruction(instruction, varTable));
            
            return instructionCode.toString();
        }
        
        //throw new NotImplementedException("BinaryOpInstruction");
    }
    */

    // BINOP Dependency (NEW) !
    public String getBinaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();
        //instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
        instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
        instructionCode.append("\t");
        instructionCode.append(instruction.getOperation()); // Maybe missing getOp()
        instructionCode.append("\n");

        return instructionCode.toString();
    }

 // BINOP critical (NEW) !!
    private String getBinaryOpInstructionCode(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: CONFIRM this
        StringBuilder instructionCode = new StringBuilder();
        OperationType binOpInst = instruction.getOperation().getOpType();

        if (binOpInst == OperationType.NOTB){
                // Maybe Missing: Reset Stack size
                cond = cond + 1;

                String leftOpName = ((Operand) instruction.getLeftOperand()).getName();
                String rightOpName = ((Operand) instruction.getRightOperand()).getName();

                instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
                if (leftOpName.equals(rightOpName))
                    instructionCode.append("\tifeq");
                else{
                    instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
                    instructionCode.append("\t");
                    instructionCode.append(getComparison(instruction.getOperation()));
                }

                instructionCode.append(" True" + cond + "\n");
                instructionCode.append("\ticonst_0" + "\n");
                instructionCode.append("\tgoto Store" + cond + "\n");
                instructionCode.append("True" + cond + ":\n");
                instructionCode.append("\ticonst_t" + "\n");
                instructionCode.append("Store" + cond + ":\n");



                return instructionCode.toString();
            }


            
        else if(binOpInst == OperationType.ANDB){
                cond = cond + 1;
                // Maybe Missing: Reset Stack size

                instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
                instructionCode.append("\tifeq False" + cond + "\n");
                instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
                instructionCode.append("\tifeq False" + cond + "\n");
                instructionCode.append("\ticonst_1" + "\n");
                instructionCode.append("\tgoto Store" + cond + "\n");
                instructionCode.append("False" + cond + ":\n");
                instructionCode.append("\ticonst_0" + "\n");
                instructionCode.append("Store" + cond + ":\n");

                return instructionCode.toString();
            }

        else if(binOpInst == OperationType.LTH){
            cond = cond + 1;
            // Maybe Missing: Reset Stack size
            instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
            instructionCode.append(loadElement(instruction.getRightOperand(), varTable));
            instructionCode.append("\t" + getComparison(instruction.getOperation()) + " True" + cond + "\n");
            instructionCode.append("\ticonst_0" + "\n");
            instructionCode.append("\tgoto Store" + cond + "\n");
            instructionCode.append("True" + cond + ":\n");
            instructionCode.append("\ticonst_1" + "\n");
            instructionCode.append("Store" + cond + ":\n");

            return instructionCode.toString();

        }

        else{ 
            instructionCode.append(loadElement(instruction.getLeftOperand(), varTable));
            // ToDo: Individually set Operation and Operand....?
            //instructionCode.append(UnaryOpInstruction(instruction, varTable));
            
            return instructionCode.toString();
        }
        
        //throw new NotImplementedException("BinaryOpInstruction");
    }











    private String getPutFieldInstructionCode(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: write this
        StringBuilder instructionCode = new StringBuilder();
        
        Element firstOperand = instruction.getFirstOperand();
        Element secondOperand = instruction.getSecondOperand();

        instructionCode.append(loadElement(firstOperand , varTable));
        instructionCode.append("\tputfield ");

        // (Maybe) Missing check to see if it's itself, aka || if ( operand.getName().equals("this") ) return className;
        instructionCode.append(((Operand) firstOperand).getName());
        instructionCode.append("/");
        instructionCode.append(((Operand) secondOperand).getName());
        instructionCode.append(" ");

        // Missing XXXXXXXXX
        //instructionCode.append(getJasminType(Field.getFieldType(instruction)));
        // Probably needs a getJasminType() somewhere
        instructionCode.append(instruction.getFieldType());
        instructionCode.append("\n");

        // (Maybe) Missing stack modifications (Maybe)

        return instructionCode.toString();
        //throw new NotImplementedException("PutFieldInstruction");
    }

    private String getGetFieldInstructionCode(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: CONFIRM
        StringBuilder instructionCode = new StringBuilder();

        Element firstOperand = instruction.getFirstOperand();
        Element secondOperand = instruction.getSecondOperand();

        instructionCode.append(loadElement(firstOperand , varTable));
        instructionCode.append("\tgetfield ");

        // Might need check to see if it's itself, aka || if ( operand.getName().equals("this") ) return className;
        instructionCode.append(((Operand) firstOperand).getName());
        instructionCode.append("/");
        instructionCode.append(((Operand) secondOperand).getName());
        instructionCode.append(" ");

        // Missing XXXXXXXXX
        // Probably needs a getJasminType() somewhere
        instructionCode.append(instruction.getFieldType());
        instructionCode.append("\n");

        // Maybe missing stack modifications

        return instructionCode.toString();
        //throw new NotImplementedException("GetFieldInstruction");
    }

    private String loadElement(Element toLoad, HashMap<String, Descriptor> varTable){
        if (toLoad.isLiteral()){
            return loadLiteral(toLoad);
        }
        else{
            return loadNonLiteral(toLoad, varTable);
        }
    }

    private String loadLiteral(Element toLoad){
        // e.g. 1, "abc", etc...

        StringBuilder literalCode = new StringBuilder();
        String literalString = ((LiteralElement) toLoad).getLiteral();

        ElementType literalType = toLoad.getType().getTypeOfElement();
        //int or bool literal
        if(literalType == INT32 || literalType == BOOLEAN){
            int literalVal = Integer.parseInt(literalString);
            //can use iconst_m1, iconst_0, ...., iconst_5 if between -1 and 5
            if(literalVal >= -1 && literalVal <= 5){
                literalCode.append("iconst_");
            }
            //can use bipush for byte: -128 - 127
            else if(literalVal >= -128 && literalVal <= 127){
                literalCode.append("bipush ");
            }
            //can use sipush for short: -32768 - 32767
            else if(literalVal >= -32768 && literalVal <= 32767){
                literalCode.append("sipush ");
            }
            //ldc for bigger literals
            else{
                literalCode.append("ldc ");
            }
        }
        // other literals use ldc
        else {
            literalCode.append("ldc ");
        }

        if(literalString == "-1")
            literalString = "m1";

        literalCode.append(literalString).append("\n");

        return literalCode.toString();
    }

    private String loadNonLiteral(Element toLoad, HashMap<String, Descriptor> varTable){
        ElementType toLoadElementType = toLoad.getType().getTypeOfElement();
        Descriptor toLoadDescriptor = varTable.get(((Operand) toLoad).getName());

        ElementType descriptorType = toLoadDescriptor.getVarType().getTypeOfElement();
        //?aload ; <a b ...> -> <b[a] ... >
        //? = i | f | d | l | a | b (= byte or boolean)`
        if(toLoadElementType != ARRAYREF && descriptorType == ARRAYREF){
            StringBuilder nonLiteralCode = new StringBuilder();
            ArrayOperand arrayOperand = (ArrayOperand) toLoad;
            Element index = arrayOperand.getIndexOperands().get(0);
            nonLiteralCode.append(loadDescriptor(toLoadDescriptor)).append(loadElement(index, varTable));
            nonLiteralCode.append("\tiaload\n");
            return nonLiteralCode.toString();
        }
        return loadDescriptor(toLoadDescriptor);
    }

    private String loadDescriptor(Descriptor toLoad){
        ElementType descriptorType = toLoad.getVarType().getTypeOfElement();
        //aload_0
        if (descriptorType == THIS){
            return "aload_0\n";
        }
        else{
            StringBuilder descriptorCode = new StringBuilder();

            int descriptorRegister = toLoad.getVirtualReg();
            switch (descriptorType){
                case INT32:
                    descriptorCode.append("i");
                    break;
                case BOOLEAN:
                    descriptorCode.append("i");
                    break;
                default:
                    descriptorCode.append("a");
                    break;
            }

            //a/i + load_1/load_2/load_3/load x
            descriptorCode.append("load");
            if(descriptorRegister <= 3){
                descriptorCode.append("_");
            }
            else{
                descriptorCode.append(" ");
            }
            descriptorCode.append(descriptorRegister).append("\n");

            return descriptorCode.toString();
        }
    }

        private String getComparison(Operation operation) {
        switch (operation.getOpType()) {
            case GTE:
                return "if_icmpge";
            case LTH:
                return "if_icmplt";
            case EQ:
                return "if_icmpeq";
            case NOTB:
            case NEQ:
                return "if_icmpne";
            default:
                System.out.println(operation.getOpType());
                return "ERROR comparison not implemented yet";
        }
    }
}