package pt.up.fe.comp.ollir;


import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Objects;

public class OllirUtils {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if(type.isArray()) {
            code.append("array.");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getOllirType(String jmmType) {

        if(jmmType.equals("void")) {
            return "V";
        }
        else if (jmmType.equals("int")) {
            return "i32";
        }

        return jmmType;
    }

    public static String createConstructor(SymbolTable symbolTable) {

        return ".construct " +
                symbolTable.getClassName() +
                "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}";
    }

    public static String createLocalFields(SymbolTable symbolTable) {
        StringBuilder fields = new StringBuilder();

        for(Symbol symbol : symbolTable.getFields()) {
            fields.append(".field ").append(getCode(symbol)).append(";\n");
        }

        return fields.toString();
    }


}
