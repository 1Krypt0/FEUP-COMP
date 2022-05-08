package AST;


import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.stream.Collectors;

public class AstUtils {

    public static Symbol builParamTypeObject(JmmNode param) {
        SpecsCheck.checkArgument(param.getKind().equals("Param"), () -> "Expected Param node, got " + param.getKind());
        SpecsCheck.checkArgument(param.getChildren().get(0).getKind().equals("Type"), () -> "Expected Type node, got " + param.getKind());

        JmmNode type = param.getChildren().get(0);
        String typeName = param.get("name");
        String paramType = type.get("type");
        boolean isArray = type.getOptional("is_array").map(Boolean::parseBoolean).orElse(false);


        return new Symbol(new Type(paramType, isArray), typeName);
    }

    public static List<Symbol> parseFields(List<JmmNode> fields) {
        // return for each field a Symbol
        return fields.stream().map(AstUtils::parseField).collect(Collectors.toList());
    }

    public static Symbol parseField(JmmNode field) {
        SpecsCheck.checkArgument(field.getKind().equals("VarDecl"), () -> "Expected Field node, got " + field.getKind());

        JmmNode type = field.getChildren().get(0);
        String typeName = field.get("name");
        String fieldType = type.get("type");
        boolean isArray = type.getOptional("is_array").map(Boolean::parseBoolean).orElse(false);

        return new Symbol(new Type(fieldType, isArray), typeName);

    }



}
