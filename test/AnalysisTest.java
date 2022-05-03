import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisTest {

    @Test
    public void test() {
        JmmSemanticsResult result = TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        System.out.println("Symbol Table: " + result.getSymbolTable());
    }
}
