package pt.up.fe.comp.analysis.semantic;

import AST.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.analysis.ProgramSymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TypeAnalyser extends PreorderJmmVisitor<List<Report>, String> {
    private final ProgramSymbolTable symbolTable;

    public TypeAnalyser(ProgramSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit(AstNode.I_D, this::visitIDs);
        addVisit(AstNode.Bin_Op, this::visitOps);
        addVisit(AstNode.Integer_Literal, this::visitInteger);
        addVisit(AstNode.True, this::visitTrue);
        addVisit(AstNode.False, this::visitFalse);
        addVisit(AstNode.Array_Access, this::visitArrayAccess);
        addVisit(AstNode.Negation, this::visitNegation);
        addVisit(AstNode.Assign, this::visitAssign);
        addVisit(AstNode.Class_Creation, this::visitClassCreation);
        addVisit(AstNode.If, this::visitBooleans);
        addVisit(AstNode.Array_Creation, this::visitArrayCreation);
        addVisit(AstNode.While_Condition, this::visitBooleans);
        addVisit(AstNode.Method_Call, this::visitMethodCall);
        addVisit(AstNode.Dot_Linked, this::visitDotLinked);
        addVisit(AstNode.Return_Statement, this::visitReturn);
        addVisit(AstNode.Length, this::visitLength);
        addVisit(AstNode.This, this::visitThis);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitThis(JmmNode node, List<Report> reports) {
        return symbolTable.getClassName();
    }

    private String visitDotLinked(JmmNode node, List<Report> reports) {
        String callerType = visit(node.getChildren().get(0), reports);
        JmmNode calledMethod = node.getChildren().get(1);

        // method from class in file
        if (symbolTable.getClassName().equals(callerType) && symbolTable.getSuper() == null) {
            if (!symbolTable.hasMethod(calledMethod.get("name"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Trying to call undefined method"));
            }
        } else if ((symbolTable.getClassName().equals(callerType) && symbolTable.getSuper() != null) || symbolTable.isImport(callerType) || callerType.equals("valid")) {
            return "valid";
        }

        return visit(calledMethod, reports);
    }

    private String visitMethodCall(JmmNode node, List<Report> reports) {
        // check for each arg if type is the same as the type in the list at that point
        String callerType = visit(node.getJmmParent().getChildren().get(node.getIndexOfSelf() - 1), reports);
        if (symbolTable.getImports().contains(callerType) || callerType.equals(symbolTable.getSuper()) || callerType.equals("valid")) {
            return "valid";
        }

        if (symbolTable.hasMethod(node.get("name"))) {
            List<Symbol> argTypes = symbolTable.getArguments(node.get("name")) == null ? Collections.emptyList() :
                    symbolTable.getArguments(node.get("name"));

            for (int i = 0; i < argTypes.size(); i++) {
                String argType = visit(node.getChildren().get(i).getChildren().get(0), reports);
                String actualArgType = argTypes.get(i).getType().isArray() ? "array" :
                        argTypes.get(i).getType().getName();
                if (!actualArgType.equals(argType)) {
                    reports.add(new Report(ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            "Invalid type for argument. Expected " + actualArgType + " and got " + argType));
                }
            }
            Type type = symbolTable.getReturnType(node.get("name"));
            return type.isArray() ? "array" : type.getName();
        }
        if (!node.getAncestor("ReturnStatement").equals(Optional.empty())) {
            JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                    node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
            Type returnType = symbolTable.getReturnType(ancestor.get("name"));
            return returnType.isArray() ? "array" : returnType.getName();
        }
        return "";
    }

    private String visitArrayCreation(JmmNode node, List<Report> reports) {
        return "array";
    }

    private String visitClassCreation(JmmNode node, List<Report> reports) {
        return node.get("name");
    }

    private String visitLength(JmmNode node, List<Report> reports) {
        node.put("type", "int");
        return "int";
    }


    private boolean variableExists(String varName, String methodName) {

        if (symbolTable.isLocalVariable(varName, methodName)) {
            return true;
        }
        if (symbolTable.hasParameter(methodName, varName)) {
            return true;
        }
        return symbolTable.isField(varName);
    }


    /**
     * Checks if IDs exist, and were created as local variables, are parameters or fields of the class
     *
     * @param node    ID to check for existence
     * @param reports Reports from semantic analysis so far
     * @return The type of the ID if it exists, and an empty String otherwise
     */
    private String visitIDs(JmmNode node, List<Report> reports) {
        final String name = node.get("name");
        JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
        String methodName = ancestor.get("name");

        if (variableExists(name, methodName)) {

            //Check if array access
            Type type = symbolTable.getVariableType(name, methodName);
            if (type.isArray()) {
                node.put("type", "array");
            } else {
                node.put("type", type.getName());
            }
            return type.isArray() ? "array" : type.getName();
        }

        if (!symbolTable.isImport(name)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "Variable " + name + " was not declared"));
        }


        return "valid";
    }

    private String visitBooleans(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);
        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
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

        String leftOperandType;
        String rightOperandType;
        if (leftOperand.getKind().equals("ID") && leftOperand.getChildren().size() > 0 && leftOperand.getChildren().get(0).getKind().equals("ArrayAccess")) {
            leftOperandType = "int";
        } else {
            leftOperandType = visit(leftOperand, reports);
        }

        if (rightOperand.getKind().equals("ID") && rightOperand.getChildren().size() > 0 && rightOperand.getChildren().get(0).getKind().equals("ArrayAccess")) {
            rightOperandType = "int";
        } else {
            rightOperandType = visit(rightOperand, reports);
        }
        // Check if operations are between objects of the same type
        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Type mismatch in operation. Can't match " + leftOperandType + " and " + rightOperandType));
        }

        String opType;
        switch (node.get("op")) {
            case "add":
            case "sub":
            case "mult":
            case "div":
                opType = "int";
                break;
            case "lt":
                if (!(leftOperandType.equals("int") && rightOperandType.equals("int"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            "Expression expected two integers"));
                }
                opType = "boolean";
                break;
            case "and":
                if (!(leftOperandType.equals("boolean") && rightOperandType.equals("boolean"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")),
                            "Expression expected two booleans"));
                }
                opType = "boolean";
                break;
            default:
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")), "Unknown operation type"));
                opType = "";
        }

        node.put("type", opType);
        return opType;
    }

    private String visitNegation(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);

        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "! Operator can only be used on boolean values"));
        }

        node.put("type", "boolean");
        return "boolean";
    }

    private String visitInteger(JmmNode node, List<Report> reports) {
        node.put("type", "int");
        return "int";
    }

    private String visitArrayAccess(JmmNode node, List<Report> reports) {
        JmmNode parentNode = node.getJmmParent();
        if (!(parentNode.getKind().equals("ID") || parentNode.getKind().equals("ArrayCreation") || parentNode.getKind().equals("Assign"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")), "Trying to access object that is not an array"));
        }

        if (parentNode.getKind().equals("ID")) {
            JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                    node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
            Type type = symbolTable.getVariableType(parentNode.get("name"), ancestor.get("name"));
            if (!type.isArray()) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")), "Trying to access object that is not an array"));
            }
        }

        String expressionType;
        if (node.getChildren().get(0).getKind().equals("ID") && node.getChildren().get(0).getChildren().size() > 0 &&
                node.getChildren().get(0).getChildren().get(0).getKind().equals("ArrayAccess")) {
            expressionType = "int";
        } else {
            expressionType = visit(node.getChildren().get(0), reports);
        }

        if (!expressionType.equals("int")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                    Integer.parseInt(node.get("col")),
                    "Invalid array access with expression of type " + expressionType));
        }

        node.put("type", "int");
        return "int";
    }

    private String visitReturn(JmmNode node, List<Report> reports) {
        String expressionType = visit(node.getChildren().get(0), reports);
        if (!expressionType.equals("valid")) {
            JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                    node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();
            Type returnType = symbolTable.getReturnType(ancestor.get("name"));
            String methodReturnType = returnType.isArray() ? "array" : returnType.getName();
            if (!expressionType.equals(methodReturnType)) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                        Integer.parseInt(node.get("col")),
                        "Invalid return type. Expected " + methodReturnType + " and got " + expressionType));
            }
        }

        return "";
    }

    private String visitAssign(JmmNode node, List<Report> reports) {

        JmmNode ancestor = node.getAncestor("MethodDeclaration").isPresent() ?
                node.getAncestor("MethodDeclaration").get() : node.getAncestor("Main").get();

        if (ancestor.get("name").equals("main")) {
            if (!symbolTable.isLocalVariable(node.get("name"), "main")) {
                if (symbolTable.isField(node.get("name"))) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                            Integer.parseInt(node.get("col")), "Static method cannot access non-static variables"));
                }
            }
        }


        Type varType = symbolTable.getVariableType(node.get("name"), ancestor.get(
                "name"));
        String type = varType.isArray() ? "array" : varType.getName();
        String assigned = visit(node.getChildren().get(0), reports);


        if (!assigned.equals("valid")) {
            if (node.getChildren().get(0).getKind().equals("ID") &&
                    node.getChildren().get(0).getChildren().size() > 0 &&
                    node.getChildren().get(0).getChildren().get(0).getKind().equals("ArrayAccess")) {
                return "";
            }
            if (node.getChildren().get(0).getKind().equals("ArrayAccess") && visit(node.getChildren().get(1),
                    reports).equals("int")) {
                return "";
            }
            if (!(symbolTable.isImport(type) && symbolTable.isImport(assigned))) {
                if (!(type.equals(assigned))) {
                    if (!(symbolTable.getClassName().equals(assigned) && type.equals(symbolTable.getSuper()))) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")),
                                Integer.parseInt(node.get("col")), "Invalid assignment of type " + assigned + " to " +
                                "variable " +
                                "of type " + type));
                    }
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
