package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class JasminTest {
    @Test
    public void test(){
        var jasminResult = TestUtils.backend(".class public HelloWorld\n");
        TestUtils.noErrors(jasminResult);
        //String result = jasminResult.compile();
    }
}
