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

    public String getFullyQualifiedName(String className){
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
    /*
    "\r\n"
    ".method public static main([Ljava/lang/String;)V\r\n"
      ".limit stack 99\r\n"
      ".limit locals 99\r\n"
      "\r\n"
      "invokestatic ioPlus/printHelloWorld()V\r\n"
      "\r\n"
      "return\r\n"
    ".end method"
    */

    public String getCode(){
        var code = new StringBuilder();
        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        var superQualifiedName = getFullyQualifiedName(classUnit.getSuperClass());
        code.append(".super ").append(superQualifiedName).append("\n");
        code.append(SpecsIo.getResource("jasminConstructor.template").replace("${SUPER_NAME}",
                superQualifiedName)).append("\n");

        for (var method : classUnit.getMethods()){
            code.append(getCode(method));
        }
        return "";
    }

    public String getCode(Method method){
        var code = new StringBuilder();

        code.append(".method public ");
        // Talvez falte um IF aqui para caso seja default (??)
        // Timestamp: 3h 43m 20s
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

        // Warning: Might be usefull to use this .return
        // Timestamp: Video 2 | 1 minute mark
        code.append("return\n.end method\n\n");

        return code.toString();

    }

    public String getCode(Instruction method){

        // -- Video Tutorial cutoff (Part1 -> Part2 tranfer), next 3 lines not visible but 4th line implemented.
        // -- Maybe changed to a standalone method?
         FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
         instructionMap.put(CallInstruction.class, this::getCode);
         instructionMap.apply(method);
        return instructionMap.apply(method);

        /*
        if(method instanceof CallInstruction){
            return getCode((CallInstruction) method);
        }

        throw new NotImplementedException(method.getClass());
         */
    }
    public String getCode(CallInstruction method){
        // Correct name? (.getInvocationType())
        switch(method.getInvocationType()){
            case invokestatic:
                return getCodeInvokeStatic(method);
            // More cases missing?
            default:
                throw new NotImplementedException(method.getInvocationType());
        }
    }

    // ------- START: Individual method type functions (cases from above function) -------

    // invokestatic
    private String getCodeInvokeStatic(CallInstruction method){
        var code = new StringBuilder();

        code.append("invokestatic ");

        return code.toString();
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