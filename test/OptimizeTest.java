
/*
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class OptimizeTest {

    public static void testJmmCompilation(String resource, String expectedOutput) {
        // If AstToJasmin pipeline, generate Jasmin
        if (TestUtils.hasAstToJasminClass()) {

            var result = TestUtils.backend(SpecsIo.getResource(resource));

            var testName = new File(resource).getName();
            System.out.println(testName + ":\n" + result.getJasminCode());
            var runOutput = result.runWithFullOutput();
            assertEquals("Error while running compiled Jasmin: " + runOutput.getOutput(), 0,
                    runOutput.getReturnValue());
            System.out.println("\n Result: " + runOutput.getOutput());

            if (expectedOutput != null) {
                assertEquals(expectedOutput, runOutput.getOutput());
            }

            return;
        }

        var result = TestUtils.optimize(SpecsIo.getResource(resource));
        var testName = new File(resource).getName();
        System.out.println(testName + ":\n" + result.getOllirCode());
    }

    public static void testJmmCompilation(String resource) {
        testJmmCompilation(resource, null);
    }



    // PASSING
    // ---------------------------------------------------------------------------------------------
    @Test
    public void testMyFile () {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/MyTestFile.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }


    // DOT LINKED
    @Test
    public void localIdCompoundTest () {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/LocalIdCompoundDotLinked.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void StaticCompoundDotLinkedTest () {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/StaticCompoundDotLinked.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void FieldAccessCompoundDotLinkedTest() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/FieldAccessCompoundDotLinked.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void DotLinkedSpecialCasesTest() {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/OtherDotLinkedCases.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }

    @Test
    public void DotLinkedCompoundOperationsTest() {
        //TODO: Fix this test
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/DotLinkedCompoundOperations.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }




    // PASSING
    // ---------------------------------------------------------------------------------------------




    /*@Test
    public void completeExample () {
        var result = TestUtils.optimize(SpecsIo.getResource("fixtures/public/cp2/CompleteExample.jmm"));
        // TestUtils.noErrors(result.getReports());
        TestUtils.noErrors(result);
    }*/


    @Test
    public void test_2_01_CompileBasic() {
        testJmmCompilation("fixtures/public/cp2/CompileBasic.jmm");
    }

    @Test
    public void test_2_02_CompileArithmetic() {
        testJmmCompilation("fixtures/public/cp2/CompileArithmetic.jmm");
    }

    @Test
    public void test_2_03_CompileMethodInvocation() {
        testJmmCompilation("fixtures/public/cp2/CompileMethodInvocation.jmm");
    }
    @Test
    public void test_2_04_CompileAssignment() {
        testJmmCompilation("fixtures/public/cp2/CompileAssignment.jmm");
    }
}
