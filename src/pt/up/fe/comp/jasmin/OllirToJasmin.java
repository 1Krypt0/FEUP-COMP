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

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit){
        this.classUnit = classUnit;
        //classUnit.show();
    }

    // TODO: change access modifiers for this class's functions
    public String getClassCode(){
        var classCode = new StringBuilder();

        // Class Name
        classCode.append(".class public ").append(classUnit.getClassName()).append("\n\n");

        for (var import2 : classUnit.getImports()){
            classCode.append(".import ");
            classCode.append(import2.toString()).append("\n");
        }
        classCode.append("\n");

        // Super Class and Constructor
        classCode.append(".super ").append(getSuperClassName()).append("\n\n");

        for (Field field : classUnit.getFields()){
            classCode.append(getFieldCode(field)).append("\n");
        }

        // Class Methods
        for (var method : classUnit.getMethods()){
            classCode.append(getMethodCode(method));
        }

        System.out.println("--------------------------------");
        System.out.println(classCode.toString());
        System.out.println("--------------------------------");

        return classCode.toString();
    }


    private String getFieldCode(Field field){
        StringBuilder fieldCode = new StringBuilder();

        fieldCode.append("\t.field");

        AccessModifiers fieldAccessModifier = field.getFieldAccessModifier();
        if(fieldAccessModifier == AccessModifiers.DEFAULT){
            throw new RuntimeException("Invalid field access modifier found: " + fieldAccessModifier);
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
        fieldCode.append(" ").append(fieldType).append(";");

        if(field.isInitialized()){
            fieldCode.append(" = ").append(field.getInitialValue());
        }

        fieldCode.append("\n");

        return fieldCode.toString();
    }

    private String getFullyQualifiedName(String className){
        if (className == null){
            throw new RuntimeException("Null class has no super class");
        }
        for(var importString : classUnit.getImports()){
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
        throw new RuntimeException("Could not find import for class " + className);
    }

    private String getSuperClassName(){
        var superClass = classUnit.getSuperClass();
        if(superClass == null){
            return "/java/lang/object";
        }
        else{
            return getFullyQualifiedName(superClass);
        }
    }

    private String getMethodAccessModifier(Method method){
        // TODO: clean up this code (*wink wink* methodAccessModifier.toLowerCase() *wink wink*)
        AccessModifiers methodAccessModifier = method.getMethodAccessModifier();
        switch (methodAccessModifier){
            case PUBLIC:
                return "public";
            case PRIVATE:
                return "private";
            case PROTECTED:
                return "protected";
            case DEFAULT:
                //throw new RuntimeException("Unknown DEFAULT access modifier");
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
                throw new NotImplementedException("Element type not implemented: " + elementType);
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

        instructionCode.append("\t.limit stack 99\n");

        instructionCode.append("invokestatic ");

        String instructionClassName = ((Operand) instruction.getFirstArg()).getName();
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

    private String getVirtualInvocationCode(CallInstruction instruction, HashMap<String, Descriptor> varTable){
        StringBuilder instructionCode = new StringBuilder();

        Element firstVirtualInstruction = instruction.getFirstArg();
        instructionCode.append(loadElement(firstVirtualInstruction, varTable));

        for (Element toLoad : instruction.getListOfOperands())
            instructionCode.append(loadElement(toLoad, varTable));

        instructionCode.append("\t.limit stack 99\n");

        instructionCode.append("invokevirtual ");

        String instructionClassName = ((Operand) instruction.getFirstArg()).getName();
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

        instructionCode.append("\t.limit stack 99\n");

        instructionCode.append("invokespecial ");

        String instructionClassName = "";
        if(instruction.getFirstArg().getType().getTypeOfElement() == THIS){
            instructionClassName = getSuperClassName();
        }
        else{
            instructionClassName = instruction.getClass().getName();
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
                instructionCode.append("\tnewarray ").append(arrayTypeString).append("\n");
                break;
            case OBJECTREF:
                String instructionClassName = ((Operand) instruction.getFirstArg()).getName();
                instructionCode.append("\tnew ").append(instructionClassName).append("\n");
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
        ElementType opType = opDest.getType().getTypeOfElement();
        int register = varTable.get(opDest.getName()).getVirtualReg();
        Descriptor opDescriptor = varTable.get(opDest.getName());
        ElementType descriptorType = opDescriptor.getVarType().getTypeOfElement();


        // TODO: when right hand side expression is a binary operation ( a = x + y, for example)

        if(descriptorType == ARRAYREF && opType != ARRAYREF){
            ArrayOperand arrayOperand = (ArrayOperand) opDest;
            Element index = arrayOperand.getIndexOperands().get(0);

            instructionCode.append(loadDescriptor(opDescriptor));
            instructionCode.append(loadElement(index, varTable));
        }

        String rhsCode = getInstructionCode(instruction.getRhs(), varTable);
        instructionCode.append(rhsCode);

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
            return "\treturn\n";
        }

        StringBuilder instructionCode = new StringBuilder();
        instructionCode.append(loadElement(instruction.getOperand(), varTable));
        ElementType returnType = instruction.getReturnType().getTypeOfElement();
        switch (returnType){
            case INT32:
                instructionCode.append("\ti");
                break;
            case BOOLEAN:
                instructionCode.append("\ta");
                break;
            default:
                throw new NotImplementedException("Unknown NEW invocation return type: " + returnType);
        }
        instructionCode.append("return\n");

        return instructionCode.toString();
    }

    private String getSingleOpInstructionCode(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable){
        return loadElement(instruction.getSingleOperand(), varTable);
        //throw new NotImplementedException("SingleOpInstruction");
    }

    private String getBinaryOpInstructionCode(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: write this
        throw new NotImplementedException("BinaryOpInstruction");
    }

    private String getPutFieldInstructionCode(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: write this
        throw new NotImplementedException("PutFieldInstruction");
    }

    private String getGetFieldInstructionCode(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable){
        //  TODO: write this
        throw new NotImplementedException("GetFieldInstruction");
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

        StringBuilder literalCode = new StringBuilder("\t");
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
        /*
        System.out.println("element | descriptor");
        System.out.println(toLoad.toString() + " | " + toLoadDescriptor.toString());
        */
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
            return "\taload_0\n";
        }
        else{
            StringBuilder descriptorCode = new StringBuilder();

            descriptorCode.append("\t");
            int descriptorRegister = toLoad.getVirtualReg();
            switch (descriptorType){
                case INT32:
                    descriptorCode.append("\ti");
                    break;
                case BOOLEAN:
                    descriptorCode.append("\ti");
                    break;
                default:
                    descriptorCode.append("\ta");
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
}