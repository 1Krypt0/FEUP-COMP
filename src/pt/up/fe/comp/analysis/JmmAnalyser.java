package pt.up.fe.comp.analysis;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.analysis.semantic.type.SemanticAnalyser;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis {
    @Override

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();

        ProgramSymbolTable symbolTable = new ProgramSymbolTable();
        SymbolTableFiller symbolTableFiller = new SymbolTableFiller();
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);
        reports.addAll(symbolTableFiller.getReports());

        SemanticAnalyser analyser = new SemanticAnalyser(symbolTable);
        analyser.visit(parserResult.getRootNode(), reports);
        System.out.println("REPORTS: " + reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
