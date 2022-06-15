package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JasminTest {
    @Test
    public void test(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminBasic.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);
    }

}
