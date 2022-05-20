package pt.up.fe.comp.jasmin;

import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.specs.util.SpecsIo;

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

        // Talvez falte um IF aqui para caso seja default (??) // Timestamp: 3h 43m 20s

        if (method.isStaticMethod()){
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        var methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");


        code.append(".end method\n\n");

        return code.toString();

    }
}

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
