package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jasmin.JasminEmitter;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminUtils;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();


        System.out.println("Parsing...");
        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        System.out.println("Analysing semantics...");
        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(analysisResult.getReports());

        JmmOptimizer optimizer = new JmmOptimizer();
        JmmSemanticsResult optimizedResult = optimizer.optimize(analysisResult);
        TestUtils.noErrors(optimizedResult);

        System.out.println("Generating code...");
        // Convert to Ollir
        var ollirResult = optimizer.toOllir(optimizedResult);

        // Instantiate JasminBackend
        var jasminEmitter = new JasminEmitter();

        // Convert to jasmin
        var jasminResult = jasminEmitter.toJasmin(ollirResult);

        if (jasminResult.getReports().size() > 0) {
            System.out.println("Something went wrong!");
            jasminResult.getReports().forEach(System.out::println);
        } else {
            System.out.println("Compilation successful!");

            SpecsIo.write(new File("./" + jasminResult.getClassName() + ".json"),
                    parserResult.toJson());

            SpecsIo.write(new File("./" + jasminResult.getClassName() + ".symbols.txt"),
                    analysisResult.getSymbolTable().print());

            SpecsIo.write(new File("./" + jasminResult.getClassName() + ".ollir"),
                    ollirResult.getOllirCode());

            var jasminFile = new File("./" + jasminResult.getClassName() + ".j");
            SpecsIo.write(jasminFile,
                    jasminResult.getJasminCode());

            JasminUtils.assemble(jasminFile, new File("./"));
        }
    }

}
