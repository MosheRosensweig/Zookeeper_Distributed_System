package edu.yu.cs.fall2019.intro_to_distributed;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class JavaRunnerImplTest
{
    public static void main(String[] args)
    {
        System.out.println(compileAndRunClass);
    }

    static String compileButNoRunClass = "package fun.with.moish;\n" +
            "public class SampleClass {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println( \"SampleClass has been compiled!\" );\n" +
            "    }\n" +
            "}";
    static String compileAndRunClass = "package fun.with.moish;\n" +
            "public class SampleClass_t {\n" +
            "    public void run() {\n" +
            "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
            "    }\n" +
            "}";
    static String compileAndRunClassNoPackage = "//package fun.with.moish;\n" +
            "public class SampleClass_t {\n" +
            "    public void run() {\n" +
            "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
            "    }\n" +
            "}";

    @Test
    public void testClassName()
    {
        JavaRunnerImpl javaRunner = new JavaRunnerImpl();
        String s = javaRunner.getClassName("/**\n" +
                " *\n" +
                " * @author XXXX\n" +
                " * Introduction: A common interface that judges all kinds of algorithm tags.\n" +
                " * some other comment\n" +
                " */\n" +
                "public class TagMatchingInterface \n" +
                "{\n" +
                "  // content\n" +
                "  public class InnerClazz{\n" +
                "    // content\n" +
                "  }\n" +
                "}");
        System.out.println(s);
        assert s.equals("TagMatchingInterface");
    }

    @Test
    public void testEnumName()
    {
        JavaRunnerImpl javaRunner = new JavaRunnerImpl();
        String s = javaRunner.getClassName("/**\n" +
                " *\n" +
                " * @author XXXX\n" +
                " * Introduction: A common interface that judges all kinds of algorithm tags.\n" +
                " * some other comment\n" +
                " */\n" +
                "public enum TagMatchingInterface \n" +
                "{\n" +
                "  // content\n" +
                "  public class InnerClazz{\n" +
                "    // content\n" +
                "  }\n" +
                "}");
        System.out.println(s);
        assert s.equals("TagMatchingInterface");
    }

    @Test @Ignore
    public void testAbstractName()
    {
        JavaRunnerImpl javaRunner = new JavaRunnerImpl();
        String s = javaRunner.getClassName("/**\n" +
                " *\n" +
                " * @author XXXX\n" +
                " * Introduction: A common interface that judges all kinds of algorithm tags.\n" +
                " * some other comment\n" +
                " */\n" +
                "public abstract class TagMatchingInterface \n" +
                "{\n" +
                "  // content\n" +
                "  public class InnerClazz{\n" +
                "    // content\n" +
                "  }\n" +
                "}");
        System.out.println(s);
        assert s.equals("TagMatchingInterface");
    }

    @Test
    public void testInterfaceName()
    {
        JavaRunnerImpl javaRunner = new JavaRunnerImpl();
        String s = javaRunner.getClassName("/**\n" +
                " *\n" +
                " * @author XXXX\n" +
                " * Introduction: A common interface that judges all kinds of algorithm tags.\n" +
                " * some other comment\n" +
                " */\n" +
                "public interface TagMatchingInterface \n" +
                "{\n" +
                "  // content\n" +
                "  public class InnerClazz{\n" +
                "    // content\n" +
                "  }\n" +
                "}");
        System.out.println(s);
        assert s.equals("TagMatchingInterface");
    }

    @Test
    public void testGetPackageName()
    {
        JavaRunnerImpl javaRunner = new JavaRunnerImpl();
        String s = javaRunner.getPackageName("package edu.yu.cs.fall2019.intro_to_distributed;\n" +
                "/**\n" +
                " *\n" +
                " * @author XXXX\n" +
                " * Introduction: A common interface that judges all kinds of algorithm tags.\n" +
                " * some other comment\n" +
                " */\n" +
                "public interface TagMatchingInterface \n" +
                "{\n" +
                "  // content\n" +
                "  public class InnerClazz{\n" +
                "    // content\n" +
                "  }\n" +
                "}");
        System.out.println(s);
        assert s.equals("edu.yu.cs.fall2019.intro_to_distributed");
    }

    @Test
    public void testStreamToString()
    {
        String fun = "this is so fun";
        InputStream stream = new ByteArrayInputStream(fun.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testCompileClassWithPathNoErrors()
    {
        String classText = compileButNoRunClass;
        String path = "way" + File.separator + "to" + File.separator + "close" + File.separator+ "fan" + File.separator;
        JavaRunnerImpl javaRunner = new JavaRunnerImpl(Paths.get(path));
        try{
            String result = javaRunner.compileAndRun(new ByteArrayInputStream(classText.getBytes(StandardCharsets.UTF_8)));
            System.out.println("result is : \n" + result);
            assertTrue("The code didn't compile when it should have", result == null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileClassAndRun()
    {
        String classText = compileAndRunClass;
        String path = "way" + File.separator + "to" + File.separator + "close" + File.separator+ "fann" + File.separator;
        JavaRunnerImpl javaRunner = new JavaRunnerImpl(Paths.get(path));
        try{
            String result = javaRunner.compileAndRun(new ByteArrayInputStream(classText.getBytes(StandardCharsets.UTF_8)));
            System.out.println("result is : \n" + result);
            assertTrue("The code didn't compile when it should have", result == null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCompileClassAndRunFinal()
    {
        String classText = compileAndRunClass;
        String path = "way" + File.separator + "to" + File.separator + "close" + File.separator+ "fann" + File.separator;
        JavaRunnerImpl javaRunner = new JavaRunnerImpl(Paths.get(path));
        try{
            String result = javaRunner.compileAndRun(new ByteArrayInputStream(classText.getBytes(StandardCharsets.UTF_8)));
            System.out.println("result is : \n" + result);
            boolean containsNoErros = result.contains("System.err:\n[]");
            boolean printOutsOk = result.contains("System.out:\n[SampleClass has been compiled and run!! For Fun?\n]");
            System.out.println(containsNoErros);
            System.out.println(printOutsOk);
            assertTrue("The code didn't compile or run ", containsNoErros&&printOutsOk);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetEmptyPackage()
    {
        String packageName = JavaRunnerImpl.getPackageName(compileAndRunClassNoPackage);
        System.out.println(compileAndRunClassNoPackage);
        System.out.println("packageName -> " + packageName);

        String clean = compileAndRunClassNoPackage.replaceAll( "//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1 " );
        System.out.println(clean);
        packageName = JavaRunnerImpl.getPackageName(clean);
        System.out.println("packageName -> " + packageName);

    }
}