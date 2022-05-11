package pt.up.fe.comp.analysis.semantic.type;

import AST.AstNode;
import pt.up.fe.comp.analysis.semantic.SemanticAnalyser;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class BooleanConditionAnalyser extends PreorderJmmVisitor<List<Report>, String> implements SemanticAnalyser {
    private final List<Report> reports;

    public BooleanConditionAnalyser() {
        reports = new ArrayList<>();
        addVisit(AstNode.ID, this::visitIDs);
        addVisit(AstNode.If, this::visitBooleans);
        addVisit(AstNode.While_Condition, this::visitBooleans);
        addVisit(AstNode.True, this::visitTrue);
        addVisit(AstNode.False, this::visitFalse);
        addVisit(AstNode.Bin_Op, this::visitOps);
        addVisit(AstNode.Negation, this::visitNegation);
        addVisit(AstNode.Integer_Literal, this::visitInteger);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitIDs(JmmNode node, List<Report> reports) {
        return "";
    }

    private String visitBooleans(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);
        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.getChildren().get(0).get("line")),
                    "Expression must evaluate to value of type bool"));
        }
        return "";
    }

    private String visitTrue(JmmNode node, List<Report> reports) {
        return "boolean";
    }

    private String visitFalse(JmmNode node, List<Report> reports) {
        return "boolean";
    }

    private String visitOps(JmmNode node, List<Report> reports) {
        JmmNode leftOperand = node.getChildren().get(0);
        JmmNode rightOperand = node.getChildren().get(1);

        String leftOperandType = visit(leftOperand, reports);
        String rightOperandType = visit(rightOperand, reports);

        if (!leftOperandType.equals(rightOperandType)) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rightOperand.get("line")),
                    "Type mismatch in operation. Can't match " + leftOperandType + " and " + rightOperandType
            ));
        }

        if (!leftOperandType.equals("int") || !rightOperandType.equals("int")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rightOperand.get("line")),
                    "Operation must be between integers. Can't match " + leftOperandType + " and " + rightOperandType
            ));
        }

        return leftOperandType;
    }

    private String visitNegation(JmmNode node, List<Report> reports) {
        String opType = visit(node.getChildren().get(0), reports);

        if (!opType.equals("boolean")) {
            reports.add(new Report(ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("line")),
                    "! Operator can only be used on boolean values"));
        }

        return "boolean";
    }

    private String visitInteger(JmmNode node, List<Report> reports) {
        return "int";
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
}
