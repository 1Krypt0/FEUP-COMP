package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator generator = new OllirGenerator(semanticsResult.getSymbolTable());
        generator.visit(semanticsResult.getRootNode());
        String code = generator.getCode();

        System.out.println("Optimized code:");
        System.out.println(code);

        return new OllirResult(semanticsResult, code, Collections.emptyList());
    }
}
