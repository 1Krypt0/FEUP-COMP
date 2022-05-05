package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImportVisitor extends AJmmVisitor<JmmNode, String> {

    private List<String> imports;
    JmmNode startNode;

    public ImportVisitor() {
        imports = new ArrayList<>();
    }

    public String getImports() {
        return String.join(".", imports);
    }

    public boolean visit(JmmNode node, ProgramSymbolTable symbolTable) {
        JmmNode startNode = node;

        System.out.println("IMPORT VISITOR" + startNode.getChildren().isEmpty());

        imports.add(startNode.get("package"));

        while (!startNode.getChildren().isEmpty()) {
            System.out.println("Added import " + startNode.get("package"));

            imports.add(startNode.get("package"));
            startNode = startNode.getChildren().get(0);
        }

        symbolTable.addImport(getImports());

        return !imports.isEmpty();
    }


}
