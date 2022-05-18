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
        code.append(SpecsIo.getResource("jasminConstructor.template").replace("${SUPER_NAME}", superQualifiedName));

        for (var method : classUnit.getMethods()){
            code.append(getCode(method));
        }
        return "";
    }
}
