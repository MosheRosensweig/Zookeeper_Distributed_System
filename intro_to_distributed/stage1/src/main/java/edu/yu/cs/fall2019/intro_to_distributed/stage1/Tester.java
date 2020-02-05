package edu.yu.cs.fall2019.intro_to_distributed.stage1;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunner;
import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Delete this class
 */
public class Tester
{
    static String cla = "package fun.with.moish;\n"+
        "public class SampleClass_t {\n"+
        "    public void run() {\n"+
        "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n"+
        "        System.err.println(\"I am not a fish man, stop saying that!\");\n"+
        "         //throw new RuntimeException(\"You are a geek\");\n"+
        "         //throw new NullPointerException(\"blaaaaaaaah\");\n"+
        "    }\n"+
        "}";
    static String cla2 = "package fun.with.moish;\n"+
            "public class SampleClass_t {\n"+
            "    public void run() {\n"+
            "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n"+
            "        //System.err.println(\"I am not a fish man, stop saying that!\");\n"+
            "         //throw new RuntimeException(\"You are a geek\");\n"+
            "         //throw new NullPointerException(\"blaaaaaaaah\");\n"+
            "    }\n"+
            "}";
    public static void main(String[] args)
    {
        /*test4();
        System.out.println("Yikes!");
        test5();*/

        //test7();
        //test8();
        test9();
    }

    private static void test9()
    {
        try {
            SimpleServer simpleServer = new SimpleServerImpl(8000);
            simpleServer.start();

            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(cla2);
            System.out.println("Got a client response");
            System.out.println("--> response code = " + r.getCode());
            System.out.println("--> src = " + r.getBody());
            System.out.println("DONE");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void test8()
    {
        try {
            SimpleServer simpleServer = new SimpleServerImpl(8000);
            simpleServer.start();

            Client client = new ClientImpl("http://localhost", 8000);
            Client.Response r = client.compileAndRun(cla);
            System.out.println("Got a client response");
            System.out.println("--> response code = " + r.getCode());
            System.out.println("--> src = " + r.getBody());
            System.out.println("DONE");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void test7()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps;
        try  {
            ps = new PrintStream(baos, true, "UTF-8");
            System.setErr(ps);
            System.out.println("err1");
            System.out.println("err twa");
            System.err.println("Err4");
            throw new RuntimeException("This is an err");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Err3");
            try{
                throw new FileNotFoundException("Hey guys");
            }catch (FileNotFoundException ex){
                ex.printStackTrace();
            }
            System.setErr(new PrintStream(
                    new FileOutputStream(FileDescriptor.err)));
            System.err.println("Err5");
        }
        String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("data =  [\n" + data + "\n]");
    }

    private static void test6()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            System.setOut(ps);
            System.out.println("Hey guys1");
            System.out.println("This si number twa");
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.out)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String data = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        System.out.println("data =  " + data);
    }

    private static void test4()
    {
        try {
            System.out.println("Woah!");
            PipedOutputStream pipeOut = new PipedOutputStream();
            PipedInputStream pipeIn = new PipedInputStream(pipeOut);
            System.setOut(new PrintStream(pipeOut));
            System.out.println("Hey yall");
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.out)));
            System.out.println(pipeIn.toString());
            StringBuilder sb = new StringBuilder();
            while(pipeIn.available() > 0){
                sb.append(pipeIn.read());
            }
            System.out.println("Teach this -> " + sb.toString());
        }catch (Exception e){
            System.out.println("cool");
        }
    }

    private static void test5()
    {
        try {
            System.out.println("Err1");
            PipedOutputStream pipeOut = new PipedOutputStream();
            PipedInputStream pipeIn = new PipedInputStream(pipeOut);
            System.setErr(new PrintStream(pipeOut));
            System.out.println("Err2");
            throw new RuntimeException("err3!!");
            //System.out.println(pipeIn.toString());
        }catch (Exception e){
            System.out.println("err4");
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.err)));

            throw new RuntimeException("err5 No way!!!");
        }
    }

    private static void test3()
    {
        StringBuilder sb = new StringBuilder();
        System.out.println(sb.toString().equals(""));

    }

    private static void test2()
    {
        try {
            String path = "way" +File.separator + "to" + File.separator + "close" + File.separator+ "fan" + File.separator;
            System.out.println(path);
            String packageName = "edu.fun.fair.wizard".replace(".", File.separator);
            String className = "Cooleo";
            String dir = path + File.separator + packageName + File.separator;
            //create the dirs//
            File file = new File(dir);
            boolean a = file.mkdirs();
            System.out.println("made files = " + a);
            System.out.println(file.getAbsoluteFile());
            System.out.println(file.getAbsolutePath());
            String classText = "public class SampleClass {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println( \"SampleClass has been compiled!\" );\n" +
                    "    }\n" +
                    "}";
            try (PrintWriter out = new PrintWriter(dir + className + ".java")) {
                out.println(classText);
            }
            File f = new File(dir + className + ".java");
            System.out.println("\t"+f.getAbsolutePath() + "\nvs\n\t" + dir + className + ".java");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void test1()
    {
        {
            Path path;
            try {
                //setup//
                path = Files.createTempDirectory(null);
                System.out.println(path);
                String packageName = "edu.fun.fair".replace(".", File.separator);
                String className = "Cool";
                String dir = path + File.separator + packageName+ File.separator;
                //create the dirs//
                File file = new File(dir);
                boolean a = file.mkdirs();
                System.out.println("made files = " +a);
                String classText = "public class SampleClass {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println( \"SampleClass has been compiled!\" );\n" +
                        "    }\n" +
                        "}";
                PrintWriter writer = new PrintWriter(className + ".java", "UTF-8");
                writer.write(classText);
                writer.close();
                //Test that it wrote it out//
                File file1 = new File(dir + className + ".java");
                System.out.println("file1 name = " + file1.getName());
                System.out.println(file1.getAbsoluteFile());
                BufferedReader br = new BufferedReader(new FileReader( className + ".java"));
                try {
                    String line = br.readLine();

                    while (line != null) {
                        System.out.println(line);
                        line = br.readLine();
                    }
                } finally {
                    br.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
