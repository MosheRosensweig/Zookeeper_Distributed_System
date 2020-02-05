package edu.yu.cs.fall2019.intro_to_distributed.stage1;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

/**
 * The following tests
 * [1] Compiles and Runs with a package
 * [2] Compiles and Runs without a package
 * [3] Compiles and Runs with a package commented out
 * [4] Doesnt' compile
 * [5] Runtime Error
 *
 * [6] Instantiation using a Path
 */
public class Stage1Test
{

    SimpleServer simpleServer;

    private void stringFormatter(String str)
    {
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
        System.out.println("<>\t" + str);
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
    }

    @Before
    public void setUp() throws Exception
    {
        simpleServer = new SimpleServerImpl(8000);
        simpleServer.start();
    }

    @After
    public void tearDown() throws Exception
    {
        simpleServer.stop();
    }

    @Test
    public void testCompileAndRunWorkingClass()
    {
        stringFormatter("testCompileAndRunWorkingClass");
        String classText = "package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "    }\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertTrue(r.getCode() == 200);
            String expected = "System.err:[]System.out:[SampleClass has been compiled and run!! For Fun?]";
            assertTrue(r.getBody().equals(expected));
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileAndRunWorkingClassNoPackage()
    {
        stringFormatter("testCompileAndRunWorkingClassNoPackage");
        String classText = "" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "    }\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertTrue(r.getCode() == 200);
            String expected = "System.err:[]System.out:[SampleClass has been compiled and run!! For Fun?]";
            assertTrue(r.getBody().equals(expected));
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileAndRunWorkingClassCommentedPackage()
    {
        stringFormatter("testCompileAndRunWorkingClassCommentedPackage");
        String classText = "//package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "    }\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertTrue(r.getCode() == 200);
            String expected = "System.err:[]System.out:[SampleClass has been compiled and run!! For Fun?]";
            assertTrue(r.getBody().equals(expected));
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileAndRunCompilerError()
    {
        stringFormatter("testCompileAndRunCompilerError");
        String classText = "/package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "    }\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertEquals(r.getCode(), 400);
            String expected = "Error on line 1, column 1, in file:///var/folders/8k/0vhzr_t1427gbptsnn06p05r0000gn/T/3808737720782016187/fun/with/moish/SampleClass_t.java";
            String expectedShort = "Error on line 1, column 1, in";
            assertTrue(r.getBody().startsWith(expectedShort));
            System.out.println("(NOTE: There may be a slight descrepency because they are created in different temporary directories)");
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileAndRunRuntimeError()
    {
        stringFormatter("testCompileAndRunRuntimeError");
        String classText = "package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "        System.err.println(\"I am not a fish man, stop saying that!\");\n" +
                "         throw new NullPointerException(\"blaaaaaaaah. Hey mom, I'm on tv\");\n" +
                "    }\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertEquals(r.getCode(), 200); //per Judah explicitly
            String expected = "System.err:[I am not a fish man, stop saying that!java.lang.NullPointerException: blaaaaaaaah. Hey mom, I'm on tv]System.out:[SampleClass has been compiled and run!! For Fun?]";
            assertTrue(r.getBody().startsWith(expected));
            System.out.println("EXPECTED:\n" + expected);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileAndRunNoRunMethod()
    {
        stringFormatter("testCompileAndRunNoRunMethod");
        String classText = "//package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    /*public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "        //System.err.println(\"I am not a fish man, stop saying that!\");\n" +
                "         //throw new RuntimeException(\"You are a geek\");\n" +
                "         //throw new NullPointerException(\"blaaaaaaaah\");\n" +
                "    }*/\n" +
                "}";
        try {
            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(classText);
            assertEquals(r.getCode(), 400); //per Judah explicitly
            String expected = "SampleClass_t.run()";
            String expected2 = "Client code does not contain a \"run\" method ";
            JavaRunnerImpl.logTest(r.getBody());
            assertTrue(r.getBody().equals(expected2));
            System.out.println("EXPECTED:\n" + expected2);
            System.out.println("ACTUAL:\n" + r.getBody());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}