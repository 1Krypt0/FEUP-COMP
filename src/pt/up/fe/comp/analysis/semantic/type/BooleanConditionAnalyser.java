package pt.up.fe.comp.analysis.semantic.type;

import AST.AstNode;
import pt.up.fe.comp.analysis.semantic.SemanticAnalyser;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.analysis.ProgramSymbolTable;

import java.util.ArrayList;
import java.util.List;

public class BooleanConditionAnalyser extends PreorderJmmVisitor<List<Report>, String> implements SemanticAnalyser {
    private final List<Report> reports;
    private final ProgramSymbolTable symbolTable;

    public BooleanConditionAnalyser(ProgramSymbolTable symbolTable) {
        reports = new ArrayList<>();
        this.symbolTable = symbolTable;
        addVisit(AstNode.I_D, this::visitIDs);
        addVisit(AstNode.If, this::visitBooleans);
        addVisit(AstNode.While_Condition, this::visitBooleans);
        addVisit(AstNode.True, this::visitTrue);
        addVisit(AstNode.False, this::visitFalse);
        addVisit(AstNode.Bin_Op, this::visitOps);
        addVisit(AstNode.Negation, this::visitNegation);
        addVisit(AstNode.Integer_Literal, this::visitInteger);
        addVisit(AstNode.Array_Access, this::visitArrayAccess);
        addVisit(AstNode.Return_Statement, this::visitReturn);
        addVisit(AstNode.Assign, this::visitAssign);
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.Length, this::visitLength);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitLength(JmmNode node, List<Report> reports) {
        node.put("type", "int");
        return "int";
    }

    private String visitDotLinked(JmmNode node, List<Report> reports) {
        String idType = visit(node.getChildren().get(0), reports);
        String calledMethodName = node.getChildren().get(1).get("name");
        String methodReturnType = visit(node.getChildren().get(1), reports);

        // If the variable is of the class Type
        if (idType.equals(symbolTable.getClassName())) {
            // And the method is unknown
            if (!symbolTable.getMethods().contains(calledMethodName)) {
                if (symbolTable.getSuper() == null) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            "Undeclared method in class " + symbolTable.getClassName()
                    ));
                    return "";
                } else {
                    return "extends";
                }
            }
        }
        return methodReturnType;
    }

    private String visitIDs(JmmNode node, List<Report> reports) {
        final String name = node.get("name");
        Symbol symbol = symbolTable.getField(name);

        if (symbol != null) {
            if (symbol.getType().isArray()) {
                node.put("type", "array");
            } else {
                node.put("type", symbol.getType().getName());
            }
            return symbol.getType().isArray() ? "array" : symbol.getType().getName();
        }

        String methodName = node.getAncestor("MethodDeclaration").get().get("name");

        symbol = symbolTable.getLocalVariable(methodName, name);

        if (symbol != null) {
            if (symbol.getType().isArray()) {
                node.put("type", "array");
            } else {
                node.put("type", symbol.getType().getName());
            }
            return symbol.getType().isArray() ? "array" : symbol.getType().getName();
        }

        symbol = symbolTable.getMethodParameter(methodName, name);

        if (symbol != null) {
            if (symbol.getType().isArray()) {
                node.put("type", "array");
            } else {
                node.put("type", symbol.getType().getName());
            }
            return symbol.getType().isArray() ? "array" : symbol.getType().getName();
        }

        reports.add(new Report(ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(node.get("line")),
                Integer.parseInt(node.get("col")),
                "Variable " + name + " does not " + "exist"));

        return "";
    }

    private String visitBooleans(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);
        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.getChildren().get(0).get("line")),
                    Integer.parseInt(node.getChildren().get(0).get("col")),
                    "Expression must evaluate to value of type bool"));
        }
        return "";
    }

    private String visitTrue(JmmNode node, List<Report> reports) {
        node.put("type", "boolean");
        return "boolean";
    }

    private String visitFalse(JmmNode node, List<Report> reports) {
        node.put("type", "boolean");
        return "boolean";
    }

    private String visitOps(JmmNode node, List<Report> reports) {
        JmmNode leftOperand = node.getChildren().get(0);
        JmmNode rightOperand = node.getChildren().get(1);

        String leftOperandType = visit(leftOperand, reports);
        String rightOperandType = visit(rightOperand, reports);

        // Check if operations are between objects of the same type
        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rightOperand.get("line")),
                    Integer.parseInt(rightOperand.get("col")),
                    "Type mismatch in operation. Can't match " + leftOperandType + " and " + rightOperandType
            ));
        }

        // TODO: 5/18/22 Figure out in what conditions this would be useful 
        /*
        if (!leftOperandType.equals("int") || !rightOperandType.equals("int")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rightOperand.get("line")),
                    Integer.parseInt(rightOperand.get("col")),
                    "Operation must be between integers. Can't match " + leftOperandType + " and " + rightOperandType
            ));
        }
         */

        node.put("type", leftOperandType);
        return leftOperandType;
    }

    private String visitNegation(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);

        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "! Operator can only be used on boolean values"));
        }

        node.put("type", "boolean");
        return "boolean";
    }

    private String visitInteger(JmmNode node, List<Report> reports) {
        node.put("type", "int");
        return "int";
    }

    private String visitArrayAccess(JmmNode node, List<Report> reports) {
        String expressionType = visit(node.getChildren().get(0), reports);

        if (!expressionType.equals("int")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Invalid array access with expression of type " + expressionType));
        }

        JmmNode parentNode = node.getJmmParent().getChildren().get(0);
        String parentType = "";
        if (parentNode.getKind().equals("ArrayAccess")) {
            parentType = "array";
        } else {
            parentType = visit(parentNode, reports);
        }

        if (!parentType.equals("array")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Trying to access object of type " + parentType));
        }

        node.put("type", "int");
        return "int";
    }

    private String visitReturn(JmmNode node, List<Report> reports) {
        String expressionType = visit(node.getChildren().get(0), reports);
        String methodName = node.getJmmParent().get("name");
        String methodReturnType = symbolTable.getReturnType(methodName).getName();

        if (!expressionType.equals("extends") && !expressionType.equals("import")) {
            if (!expressionType.equals(methodReturnType)) {
                reports.add(new Report(ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Invalid return type. Expected " + methodReturnType + " and got " + expressionType));
            }
        }
        return methodReturnType;
    }

    private String visitAssign(JmmNode node, List<Report> reports) {
        String varName = node.get("name");
        String methodName = getMethodName(node);
        String varType = symbolTable.getLocalVariable(methodName, varName).getType().getName();
        String assignedType = "";
        if (node.getChildren().get(0).getKind().equals("ClassCreation")) {
            assignedType = node.getChildren().get(0).get("name");
        } else {
            assignedType = visit(node.getChildren().get(0), reports);
        }
        if (!(symbolTable.getImports().contains(assignedType) && symbolTable.getImports().contains(varType))) {
            if (!varType.equals(assignedType)) {
                String classExtension = getClassExtension(node);
                if (!assignedType.equals(classExtension)) {
                    reports.add(new Report(ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            "Invalid assignment of type " + assignedType + " to variable of type " + varType));
                }
            }
        }
        return "";
    }

    private String defaultVisit(JmmNode node, List<Report> reports) {
        for (JmmNode child : node.getChildren()) {
            visit(child, reports);
        }
        return "";
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }

    private String getMethodName(JmmNode node) {
        if (node.getJmmParent().getKind().equals("MethodDeclaration")) {
            return node.getJmmParent().get("name");
        }
        return getMethodName(node.getJmmParent());
    }

    private String getClassExtension(JmmNode node) {
        if (node.getJmmParent().getKind().equals("ClassDecl")) {
            if (node.getJmmParent().getAttributes().contains("extended_class")) {
                return node.getJmmParent().get("extended_class");
            } else {
                return "";
            }
        }
        return getClassExtension(node.getJmmParent());
    }
}
