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

    private final ProgramSymbolTable symbolTable;


    public MethodAnalyser(ProgramSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit(AstNode.Method_Call, this::visitMethodCalls);
        setDefaultVisit(this::defaultVisit);
    }

    /**
     * Checks if the method is being called from the class in the file where it is declared.
     *
     * @param node method being called
     * @return true if method is being called by the same class, false otherwise
     */
    private boolean isCallingFromSelf(JmmNode node, JmmNode caller) {
        String id = caller.getAttributes().contains("name") ? caller.get("name") : "";
        JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
        if (caller.getKind().equals("This") || id.equals(symbolTable.getClassName())) {
            return true;
        } else if (caller.getKind().equals("ID")) { // Check if variable is of class in file
            String varType = getVariableType(id, ancestor.get("name"));
            if (!(varType == null)) {
                return varType.equals(symbolTable.getClassName());
            }
        }
        return false;
    }

    private boolean isExtension(JmmNode node) {
        return node.getAncestor("ClassDecl").get().getAttributes().contains("extended_class");
    }

    private boolean isImported(JmmNode node, JmmNode caller) {

        String id = caller.getAttributes().contains("name") ? caller.get("name") : "";
        if (symbolTable.getImports().contains(id)) {
            return true;
        }
        JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
        String type = getVariableType(id, ancestor.get("name"));
        if (!(type == null)) {
            return symbolTable.getImports().contains(type);
        }

        return false;
    }

    private String visitMethodCalls(JmmNode node, List<Report> reports) {
        // Check if the method exists or is from imported/super class
        int idx = node.getIndexOfSelf();
        JmmNode caller = node.getJmmParent().getChildren().get(idx - 1);

        if (isCallingFromSelf(node, caller)) {

            if (mainCallingByThis(node, caller)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")), "Trying to call non-static method inside static method " +
                        "main"));
            }

            if (!symbolTable.hasMethod(node.get("name"))) {
                if (!isExtension(node)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Call to undeclared method in class"));
                }
            } else {
                // Check for arguments
                if (!hasCorrectArguments(node)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt((node.get("line"))),
                            Integer.parseInt(node.get("col")), "Number of arguments does not match method " +
                            "declaration"));
                }
            }
        } else if (!isImported(node, caller) && !caller.getKind().equals("DotLinked")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "Cannot call method. Class was not imported"));
        }

        return "";
    }

    private boolean mainCallingByThis(JmmNode node, JmmNode caller) {
        return caller.getKind().equals("This") && node.getAncestor("Main").isPresent();
    }

    /**
     * Checks if a method defined in the file has the correct arguments amount
     *
     * @param node Method being called
     * @return true if there are the correct amount of arguments, false
     * otherwise
     */
    private boolean hasCorrectArguments(JmmNode node) {
        List<Symbol> parameters = symbolTable.getParameters(node.get("name"));
        List<JmmNode> args = node.getChildren();

        return parameters.size() == args.size();
    }

    /**
     * Fetches the type of the variable from the symbol table, be it a field of the class, a local variable of the
     * method or a parameter from the method
     *
     * @param varID           The variable name
     * @param methodSignature The method in which it is mentioned
     * @return The type of the variable if the variable is found, null otherwise
     */
    private String getVariableType(String varID, String methodSignature) {
        boolean isLocalVar = symbolTable.hasLocalVariable(methodSignature, varID);
        if (isLocalVar) {
            return symbolTable.getLocalVariable(methodSignature, varID).getType().getName();
        }
        boolean isParameter = symbolTable.hasParameter(methodSignature, varID);
        if (isParameter) {
            return symbolTable.getMethodParameter(methodSignature, varID).getType().getName();
        }
        boolean isField = symbolTable.hasField(varID);
        if (isField) {
            return symbolTable.getField(varID).getType().getName();
        }
        return null;
    }


    private String defaultVisit(JmmNode node, List<Report> reports) {
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }
        return "";
    }
}
