package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.ElementType.ARRAYREF;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit){
        this.classUnit = classUnit;
    }

    // TODO: change access modifiers for this class's functions
    public String getClassCode(){
        var classCode = new StringBuilder();

        // Class Name
        classCode.append(".class public ").append(classUnit.getClassName()).append("\n\n");

        //
        // TODO: imports
        //
        for (var import2 : classUnit.getImports()){
            classCode.append(".import ");
            classCode.append(import2.toString()).append("\n");
         }
         classCode.append("\n");

        // Super Class and Constructor
        // TODO: separate getting super and building the constructor
        //              constructor can now be built using the standard method generation function
        classCode.append(getClassSuper());

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

    public String getClassSuper(){
        var classCode = new StringBuilder();

        // Super Class
        String superClassQualifiedName;
        var superClass = classUnit.getSuperClass();
        if(superClass == null){
            superClassQualifiedName = "/java/lang/object";
        }
        else{
            superClassQualifiedName = getFullyQualifiedName(superClass);
        }
        classCode.append(".super ").append(superClassQualifiedName).append("\n\n");

        return classCode.toString();
    }

    // TODO: clean up this code (*wink wink* methodAccessModifier.toLowerCase() *wink wink*)
    public String getMethodAccessModifier(Method method){
        AccessModifiers methodAccessModifier = method.getMethodAccessModifier();
        //System.out.println(method.getMethodName() + "     |      " + methodAccessModifier);
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

        if(elementType == ARRAYREF){
            // check type of array items
            ElementType childElemType = ((ArrayType) type).getTypeOfElements();
            typeDescriptor.append("[").append(getJasminElementType(childElemType));
            return typeDescriptor.toString();
        }
        else{
            typeDescriptor.append(getJasminElementType(elementType));
            return typeDescriptor.toString();
        }
        /*
        // TODO: Object types
        //if(elementType == OBJECTREF){
        //
        //}
        // TODO: Class types
        //if(elementType == CLASS){
        //
        //}
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

    public String getMethodBody(Method method){
        StringBuilder bodyCode = new StringBuilder();

        // TODO: method fields
        HashMap<String, Descriptor> methodVarTable = method.getVarTable();

        // TODO: remainder of the code
        StringBuilder instructions = new StringBuilder();
        HashMap<String, Instruction> labels = method.getLabels();

        return bodyCode.toString();
    }

    public String getMethodCode(Method method){
        var methodCode = new StringBuilder();
        methodCode.append(getMethodHeader(method));
        methodCode.append(getMethodBody(method));
        methodCode.append(".end method\n\n");
        return methodCode.toString();
    }

    // ------------------------------------------   OLD CODE ------------------------
    /*

    public String getInstructionCode(Instruction method){
         FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
         //instructionMap.put(CallInstruction.class, this::getCode);
         instructionMap.apply(method);
        return instructionMap.apply(method);
    }

    public String getCallInstructionCode(CallInstruction method){
        switch(method.getInvocationType()){
            case invokestatic:
                return getCodeInvokeStatic(method);
            // More cases missing?
            default:
                throw new NotImplementedException(method.getInvocationType());
        }
    }

    // invokestatic
    private String getCodeInvokeStatic(CallInstruction method){
        var code = new StringBuilder();
        //method.show();
        code.append("invokestatic ");

        var methodClass = ((Operand) method.getFirstArg()).getName();

        code.append(getFullyQualifiedName(methodClass));
        code.append("/");

        // Warning:
        // .getLiteral() pelos vistos da return com aspas "" entre o que queremos, ent usei um substring para as
        // retirar. Not sure se isto acontece sempre, talvez temos de ter outra maneira de solucionar, porque senao
        // estariamos a remover partes legit do SecondArg do method.
        var calledMethod = ((LiteralElement) method.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1, calledMethod.length() - 1));

        code.append("(");
        // method.getListOfOperands();
        for(var operand : method.getListOfOperands()){
            getArgumentCode(operand);
        }
        code.append(")");
        code.append(getJasminType(method.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    private void getArgumentCode(Element operand){
        throw new NotImplementedException(this);
    }
    // ------- END: Individual method type functions (cases from above function) -------
     */
}