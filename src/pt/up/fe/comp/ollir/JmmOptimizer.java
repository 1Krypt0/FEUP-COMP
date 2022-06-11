package pt.up.fe.comp.ollir;

import pt.up.fe.comp.analysis.ProgramSymbolTable;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator generator = new OllirGenerator((ProgramSymbolTable) semanticsResult.getSymbolTable());
        generator.visit(semanticsResult.getRootNode());
        String code = generator.getCode();

         String[] lines = code.split("\n");
         for (int i = 0; i < lines.length; i++) {
             System.out.println(i + ": " + lines[i]);
         }


        return new OllirResult(semanticsResult, code, Collections.emptyList());
    }
}
