package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.election.Vote;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
@SuppressWarnings("unchecked")
public class Stage3Test
{
    private int gatewayPort = 7095;
    private Client client = null;
    @Test
    public void testAllHaveTheSameLeader() throws InterruptedException
    {
        //create IDs and addresses
        HashMap<Long, InetSocketAddress> peerIDtoAddress = new HashMap<>(3);
        peerIDtoAddress.put(1L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(2L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(3L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(4L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(5L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(6L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(7L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(8L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));

        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for(Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet())
        {
            HashMap<Long, InetSocketAddress> copy = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            ConcurrentHashMap<Long, InetSocketAddress> map = new ConcurrentHashMap<>();
            map.putAll(copy);
            map.remove(entry.getKey());
            ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        }
        //wait for threads to start.
        try
        {
            boolean moveOn = false;
            while (!moveOn) {
                Thread.sleep(500);
                int count = 0;
                for (ZooKeeperPeerServer server : servers) if(server.getCurrentLeader() != null) count++;
                //if(count >= (servers.size()/2+1)) moveOn = true;
                if(count >= (servers.size())) moveOn = true; //wait for all servers
            }
        }
        catch (Exception e)
        {
        }
        System.out.println("Numbers of servers = " + servers.size() + ",  quorum size = " + servers.get(0).getQuorumSize());
        long lead = -1;
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers)
        {
            Vote leader = server.getCurrentLeader();
            if (leader != null)
            {
                System.out.println("Server on port " + server.getMyAddress().getPort() + " whose ID is " + server.getId() +
                        " has the following ID as its leader: " + leader.getCandidateID() + " and its state is " + server.getPeerState().name());
                //server.shutdown();
                lead = server.getCurrentLeader().getCandidateID();
            }
        }
        for (ZooKeeperPeerServer server : servers){
            if(server.getId() == lead) assertEquals("The leader " + server.getId() + " isn't leading",
                    server.getPeerState(), ZooKeeperPeerServer.ServerState.LEADING);
            else {
                assertEquals("The Follower " + server.getId() + " is supposed to be the FOLLOWING state",
                        ZooKeeperPeerServer.ServerState.FOLLOWING, server.getPeerState());
                assertEquals("The Follower " + server.getId() + " is supposed to be following server " + lead,
                        server.getCurrentLeader().getCandidateID(), lead);
            }
        }
         //give the system time to setup
        Thread.sleep(3_000);
        testCompileAndRunWorkingClass();
        testCompileAndRunWorkingClassNoPackage();
        testCompileAndRunWorkingClassCommentedPackage();
        testCompileAndRunCompilerError();
        testCompileAndRunRuntimeError();
        testCompileAndRunNoRunMethod();
    }

    private void stringFormatter(String str)
    {
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
        System.out.println("<>\t" + str);
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
    }

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
            client = new ClientImpl("http://localhost", gatewayPort);
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
            //Client client = new ClientImpl("http://localhost", 8000);
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
            //Client client = new ClientImpl("http://localhost", 8000);
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
            //Client client = new ClientImpl("http://localhost", 8000);
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
            //Client client = new ClientImpl("http://localhost", 8000);
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
            //Client client = new ClientImpl("http://localhost", 8000);
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
