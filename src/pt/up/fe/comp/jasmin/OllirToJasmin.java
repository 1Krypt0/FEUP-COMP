package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;

    public OllirToJasmin(ClassUnit classUnit){
        this.classUnit = classUnit;
    }

    public String getClassCode(){
        var code = new StringBuilder();

        // Class Name
        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        //
        // TODO: imports
        //

        // Super Class and Constructor
        code.append(getClassSuperAndConstructor());
        // Class Fields
/*
        // Class Methods
        for (var method : classUnit.getMethods()){
            code.append(getMethodCode(method));
        }
 */
        return code.toString();
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

    public String getClassSuperAndConstructor(){
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

        //  CONSTRUCTOR
        classCode.append(".method public <init>()V\n");
        classCode.append("\taload_0\n");
        classCode.append("\tinvokenonvirtual " + superClassQualifiedName + "/<init>()V\n");
        classCode.append("\treturn\n");
        classCode.append(".end method\n");

        return classCode.toString();
    }

    public String getMethodCode(Method method){
        var code = new StringBuilder();

        code.append(".method public ");
        if (method.isStaticMethod()){
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        var methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");
        code.append(".limit stack 99\n");
        code.append(".limit locals 99\n");

        for(var inst : method.getInstructions()){
            code.append(getCode(inst));
        }

        code.append("return\n.end method\n\n");

        return code.toString();

    }

    public String getInstructionCode(Instruction method){
         FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
         instructionMap.put(CallInstruction.class, this::getCode);
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


    public String getJasminType(Type type){
        if (type instanceof ArrayType){
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }

        return getJasminType(type.getTypeOfElement());

    }

    public String getJasminType(ElementType type){
        switch(type){
        case STRING:
            return "Ljava/lang/String;";
        case VOID:
            return "V";
        default:
            throw new NotImplementedException(type);
        }
    }
}