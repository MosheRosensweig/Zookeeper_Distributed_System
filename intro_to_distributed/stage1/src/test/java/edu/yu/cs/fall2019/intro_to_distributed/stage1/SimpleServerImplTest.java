package edu.yu.cs.fall2019.intro_to_distributed.stage1;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class SimpleServerImplTest
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

    @Test
    public void testServerAndClient()
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
            r = client.compileAndRun(cla);
            System.out.println("Got a client response");
            System.out.println("--> response code = " + r.getCode());
            System.out.println("--> src = " + r.getBody());
            System.out.println("DONE");
            client = new ClientImpl("http://localhost", 8000);
            r = client.compileAndRun(cla);
            System.out.println("Got a client response");
            System.out.println("--> response code = " + r.getCode());
            System.out.println("--> src = " + r.getBody());
            System.out.println("DONE");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testServerStop()
    {
        try{
            SimpleServer simpleServer = new SimpleServerImpl(8000);
            simpleServer.start();
            simpleServer.stop();
        }catch (Exception e){

        }
    }
}