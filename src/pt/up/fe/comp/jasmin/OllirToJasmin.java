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
            classCode.append("\t").append(getFieldCode(field)).append("\n");
        }

        // Class Methods
        for (var method : classUnit.getMethods()){
            classCode.append(getMethodCode(method));
        }

        return classCode.toString();
    }


    public String getFieldCode(Field field){
        StringBuilder fieldCode = new StringBuilder();

        fieldCode.append("\t.field");

        AccessModifiers fieldAccessModifier = field.getFieldAccessModifier();
        if(fieldAccessModifier == AccessModifiers.DEFAULT){
            throw new RuntimeException("Invalid field access modifier found");
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

    public String getFullyQualifiedName(String className){

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

    public String getSuperClassName(){
        var superClass = classUnit.getSuperClass();
        if(superClass == null){
            return "/java/lang/object";
        }
        else{
            return getFullyQualifiedName(superClass);
        }
    }

    // TODO: clean up this code (*wink wink* methodAccessModifier.toLowerCase() *wink wink*)
    public String getMethodAccessModifier(Method method){
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

    public String getJasminElementType(ElementType elementType){
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

    public String getJasminType(Type type){
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

    public String getMethodParameters(Method method){
        StringBuilder methodParametersString = new StringBuilder();

        ArrayList<Element> methodParameters = method.getParams();
        for (Element parameter : methodParameters){
            var parameterType = getJasminType(parameter.getType());
            methodParametersString.append(parameterType);
        }

        return methodParametersString.toString();
    }

    public String getMethodHeader(Method method){
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

    public String getInstructionCode(Instruction instruction){
        InstructionType instructionType = instruction.getInstType();

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
    }

    public String getCallInstructionCode(CallInstruction instruction){
        CallType methodInvocationType = instruction.getInvocationType();
        switch (methodInvocationType){
            case invokestatic:
                return getStaticInvocationCode(instruction);
            case invokevirtual:
                return getVirtualInvocationCode(instruction);
            case invokeinterface:
                throw new NotImplementedException("invokeinterface method invocation");
            case invokespecial:
                return getInvokeSpecialCode(instruction);
            case NEW:
                throw new NotImplementedException("NEW method invocation");
            case arraylength:
                throw new NotImplementedException("arraylength method invocation");
            case ldc:
                throw new NotImplementedException("ldc method invocation");
            default:
                throw new NotImplementedException("Uknown method invocation type: " + methodInvocationType);
        }
    }

    public String getStaticInvocationCode(CallInstruction instruction){
        StringBuilder instructionCode = new StringBuilder();

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

    public String getVirtualInvocationCode(CallInstruction instruction){
        StringBuilder instructionCode = new StringBuilder();

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

    public String getInvokeSpecialCode(CallInstruction instruction){
        StringBuilder instructionCode = new StringBuilder();

        instructionCode.append("invokespecial ");

        String instructionClassName = "";
        if(instruction.getFirstArg().getType().getTypeOfElement() == THIS){
            instructionClassName = getFullyQualifiedName(getSuperClassName());
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

    public String getAssignInstructionCode(AssignInstruction instruction){
        throw new NotImplementedException("AssignInstruction");
    }

    public String getReturnInstructionCode(ReturnInstruction instruction){
        throw new NotImplementedException("ReturnInstruction");
    }

    public String getSingleOpInstructionCode(SingleOpInstruction instruction){
        throw new NotImplementedException("SingleOpInstruction");
    }

    public String getBinaryOpInstructionCode(BinaryOpInstruction instruction){
        throw new NotImplementedException("BinaryOpInstruction");
    }

    public String getPutFieldInstructionCode(PutFieldInstruction instruction){
        throw new NotImplementedException("PutFieldInstruction");
    }

    public String getGetFieldInstructionCode(GetFieldInstruction instruction){
        throw new NotImplementedException("GetFieldInstruction");
    }

    public String getMethodBody(Method method){
        StringBuilder bodyCode = new StringBuilder();

        bodyCode.append("\t.limit stack 99\n");
        bodyCode.append("\t.limit locals 99\n");

        for(Instruction inst : method.getInstructions()){
            bodyCode.append("\t").append(getInstructionCode(inst));
        }

        return bodyCode.toString();
    }

    public String getMethodCode(Method method){
        var methodCode = new StringBuilder();
        methodCode.append(getMethodHeader(method));
        methodCode.append(getMethodBody(method));
        methodCode.append(".end method\n\n");
        return methodCode.toString();
    }
}