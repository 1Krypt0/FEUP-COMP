package pt.up.fe.comp.analysis.semantic;

import AST.AstNode;
import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodAnalyser extends PreorderJmmVisitor<List<Report>, String> {

    private final List<Report> reports;
    private final ProgramSymbolTable symbolTable;

    public MethodAnalyser(ProgramSymbolTable symbolTable) {
        this.reports = new ArrayList<>();
        this.symbolTable = symbolTable;
        addVisit(AstNode.Method_Call, this::visitMethodCalls);
        setDefaultVisit(this::defaultVisit);
    }

    public List<Report> getReports() {
        return this.reports;
    }

    private String visitMethodCalls(JmmNode node, List<Report> reports) {
        int idx = node.getIndexOfSelf();
        JmmNode caller = node.getJmmParent().getChildren().get(idx - 1);
        String id = caller.getAttributes().contains("name") ? caller.get("name") : "";
        if (caller.getKind().equals("This") || id.equals(symbolTable.getClassName())) {
            // Method not defined in class
            if (!symbolTable.hasMethod(node.get("name"))) {
                // Check for extension
                if (!node.getAncestor("ClassDecl").get().getAttributes().contains("extended_class")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Call to undeclared method in class"));
                }
            }
        } else {
            // can also be a variable id
            String varType = getVariableType(id, node.getAncestor("MethodDeclaration").get().get("name"));
            if (!(varType == null)) {
                if (varType.equals(symbolTable.getClassName())) {
                    if (!symbolTable.hasMethod(node.get("name"))) {
                        if (!node.getAncestor("ClassDecl").get().getAttributes().contains("extended_class")) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                                    Integer.parseInt(node.get("col")), ""));
                        }
                    }
                }
            } else {
                // Check if class is defined in imports, if it is, assume it is correct
                if (!symbolTable.getImports().contains(id)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Cannot call method. Class was not imported"));
                }
            }
        }


        return null;
    }

    private String getVariableType(String varID, String methodSignature) {
        boolean isField = symbolTable.hasField(varID);
        if (isField) {
            return symbolTable.getField(varID).getType().getName();
        } else {
            boolean isParameter = symbolTable.hasParameter(methodSignature, varID);
            if (isParameter) {
                return symbolTable.getMethodParameter(methodSignature, varID).getType().getName();
            } else {
                boolean isLocalVar = symbolTable.hasLocalVariable(methodSignature, varID);
                if (isLocalVar) {
                    return symbolTable.getLocalVariables(methodSignature, varID).getType().getName();
                } else {
                    return null;
                }
            }
        }
    }

    private String defaultVisit(JmmNode node, List<Report> reports) {
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }
        return "";
    }
}
