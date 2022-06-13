package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;

public class ImportVisitor extends AJmmVisitor<JmmNode, String> {

    private final List<String> imports;

    public ImportVisitor() {
        imports = new ArrayList<>();
    }

    public String getImports() {
        return String.join(".", imports);
    }

    public boolean visit(JmmNode node, ProgramSymbolTable symbolTable) {
        JmmNode startNode = node;

        imports.add(startNode.get("package"));

        while (!startNode.getChildren().isEmpty()) {
            startNode = startNode.getChildren().get(0);
            imports.add(startNode.get("package"));
        }

        symbolTable.addImport(getImports());

        return !imports.isEmpty();
    }

}
