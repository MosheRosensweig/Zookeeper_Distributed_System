package edu.yu.cs.fall2019.intro_to_distributed.stage4;

import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.election.Vote;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import org.junit.After;
import org.junit.Before;
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

public class OldStage4Test
{
    ClientImpl client = null;
    int gatewayClientPort = 7095;

    @Before
    public void setUp() throws Exception
    {
        client = new ClientImpl("http://localhost", gatewayClientPort);
    }

    @After
    public void tearDown() throws Exception
    {
        Thread.sleep(2_000);
    }

    private void assertPeerListIsUpdatedCorrectly(ZooKeeperPeerServer server)
    {
        assertEquals("Leader should be 6", server.getCurrentLeader().getCandidateID(), 6l);
        assertEquals("Epoch should be 2", server.getPeerEpoch(), 2);
        Map<Long, InetSocketAddress> map = server.getPeerIDtoAddress();
        long my_ID = server.getId();
        if(my_ID != 1) assert(map.containsKey(1l));
        if(my_ID != 2) assert(map.containsKey(2l));
        if(my_ID != 3) assert(map.containsKey(3l));
        if(my_ID != 4) assert(map.containsKey(4l));
        if(my_ID != 6) assert(map.containsKey(6l));
        if(my_ID != 9) assert(map.containsKey(9l));

        assert(!map.containsKey(5l));
        assert(!map.containsKey(7l));
        assert(!map.containsKey(8l));
    }

    @Test
    public void testWithAClient() throws InterruptedException
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
        peerIDtoAddress.put(9L, new InetSocketAddress("localhost", GateWay.GATEWAY_PORT));
        //peerIDtoAddress.put(9l, new InetSocketAddress("localhost", GateWay.GATEWAY_PORT));
        //create servers
        ArrayList<ZooKeeperPeerServer> servers = new ArrayList<>(3);
        for (Map.Entry<Long, InetSocketAddress> entry : peerIDtoAddress.entrySet()) {
            HashMap<Long, InetSocketAddress> copy = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            ConcurrentHashMap<Long, InetSocketAddress> map = new ConcurrentHashMap<>();
            map.putAll(copy);
            map.remove(entry.getKey());
            ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        }
        //wait for threads to start.
        try {
            boolean moveOn = false;
            while (!moveOn) {
                System.out.println("waiting...");
                Thread.sleep(500);
                int count = 0;
                for (ZooKeeperPeerServer server : servers) if (server.getCurrentLeader() != null) count++;
                //if(count >= (servers.size()/2+1)) moveOn = true;
                if (count >= (servers.size())) moveOn = true; //wait for all servers
            }
        } catch (Exception e) {
        }
        System.out.println("Numbers of servers = " + servers.size() + ",  quorum size = " + servers.get(0).getQuorumSize());
        long lead = -1;
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers) {
            Vote leader = server.getCurrentLeader();
            if (leader != null) {
                System.out.println("Server on port " + server.getMyAddress().getPort() + " whose ID is " + server.getId() +
                        " has the following ID as its leader: " + leader.getCandidateID() + " and its state is " + server.getPeerState().name());
                //server.shutdown();
                lead = server.getCurrentLeader().getCandidateID();
            }
        }
        for (ZooKeeperPeerServer server : servers) {
            if (server.getId() == lead) assertEquals("The leader " + server.getId() + " isn't leading",
                    server.getPeerState(), ZooKeeperPeerServer.ServerState.LEADING);
            else if (server.getMyPort() == GateWay.GATEWAY_PORT) {
                assertEquals("The Gateway with sid:" + server.getId() + " is supposed to be the OBSERVING state",
                        ZooKeeperPeerServer.ServerState.OBSERVING, server.getPeerState());
                assertEquals("The Gateway with sid:" + server.getId() + " is supposed to be following server " + lead,
                        server.getCurrentLeader().getCandidateID(), lead);
            } else {
                assertEquals("The Follower " + server.getId() + " is supposed to be the FOLLOWING state",
                        ZooKeeperPeerServer.ServerState.FOLLOWING, server.getPeerState());
                assertEquals("The Follower " + server.getId() + " is supposed to be following server " + lead,
                        server.getCurrentLeader().getCandidateID(), lead);
            }
        }
        //give the system time to setup
        Thread.sleep(3_000);
        ZooKeeperPeerServer zooKeeperPeerServer = null;
        //get the leader server
        for (ZooKeeperPeerServer z : servers) if (z.getId() == 8) zooKeeperPeerServer = z;
        //send a client request
        System.out.println("Sending a client request (right before the leader dies)");
        testCompileAndRunWorkingClass();
        //kill the leader
        System.out.println("\nKilling the leader\n");
        Thread.sleep(2_000);
        zooKeeperPeerServer.shutdown();

        for (ZooKeeperPeerServer z : servers) if (z.getId() == 5) zooKeeperPeerServer = z;
        System.out.println("\nKilling sever 5 (port:" + zooKeeperPeerServer.getMyPort() + ")\n");
        Thread.sleep(3_000);
        zooKeeperPeerServer.shutdown();

        //watch a new election
        Thread.sleep(15_000);

        for (ZooKeeperPeerServer z : servers) if (z.getId() == 7) zooKeeperPeerServer = z;
        System.out.println("\nKilling sever new leader, 7 (port:" + zooKeeperPeerServer.getMyPort() + ")\n");
        Thread.sleep(3_000);
        zooKeeperPeerServer.shutdown();

        //watch a new election
        Thread.sleep(15_000);
        testCompileAndRunWorkingClass();
        Thread.sleep(5_000);
        System.out.println("end of test");

        for (ZooKeeperPeerServer server : servers) {
            System.out.println("Server id:" + server.getId() + ", port:" + server.getMyPort()
                    + " is " + server.getPeerState() + " and is following sid:" + server.getCurrentLeader().getCandidateID() + ", epoch = " + server.getPeerEpoch() + ", quarumSize:" + server.getQuorumSize());
        }
        for (ZooKeeperPeerServer server : servers) {
            System.out.println("Server id:" + server.getId() + ", port:" + server.getMyPort()
                    + " map = " + server.getPeerIDtoAddress());
        }
        //make sure everything is correct
        /**
         * I.e. based on the test above and normal timing, the below should be true.
         * Note: Server 8 is the first leader and dies
         * Note: Server 5 dies before it can detect the leader died
         * Note: Server 7 dies as a leader, after having detected both 8 and 5 died
         * Note: Server 6 takes over and is the leader in the end
         *
         * Note: The peerList is updated - i.e. servers are removed when they are deleted.
         */
        for (ZooKeeperPeerServer server : servers) {
            long id = server.getId();
            int asInt = (int) id;
            switch (asInt) {
                case 9:
                    assertEquals("Gateway Should be Observing", server.getPeerState(), ZooKeeperPeerServer.ServerState.OBSERVING);
                    assertPeerListIsUpdatedCorrectly(server);
                    break;
                case 8:
                    assertEquals("Leader should be 8", server.getCurrentLeader().getCandidateID(), 8l);
                    assertEquals("Epoch should be 2", server.getPeerEpoch(), 0);
                    break;
                case 7:
                    assertEquals("Leader should be 8", server.getCurrentLeader().getCandidateID(), 7l);
                    assertEquals("Epoch should be 2", server.getPeerEpoch(), 1);
                    break;
                case 6:
                    assertEquals("Leader Should be Leading", server.getPeerState(), ZooKeeperPeerServer.ServerState.LEADING);
                    assertPeerListIsUpdatedCorrectly(server);
                    break;
                case 5:
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                    assertEquals("Follower Should be Following", server.getPeerState(), ZooKeeperPeerServer.ServerState.FOLLOWING);
                    assertPeerListIsUpdatedCorrectly(server);
                    break;
            }
        }

        for (ZooKeeperPeerServer z : servers) if (z.getId() == 6) zooKeeperPeerServer = z;
        testBeforeAndAferLeader(zooKeeperPeerServer);
        System.out.println("\n Test Concluded");
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

    private void stringFormatter(String str)
    {
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
        System.out.println("<>\t" + str);
        System.out.println("<><><><><><><><><><><><><><><><><><><><><><><>");
    }

    /**
     * Send a message, kill the leader, and then get the response
     * @param server
     */
    private void testBeforeAndAferLeader(ZooKeeperPeerServer server)
    {
        stringFormatter("Test: Send Client message, kill the leader and then try to get a response");
        String classText = "package fun.with.moish;\n" +
                "public class SampleClass_t {\n" +
                "    public void run() {\n" +
                "        System.out.println( \"SampleClass has been compiled and run!! For Fun?\" );\n" +
                "    }\n" +
                "}";
        try {
            Client.Response r = client.compileAndRun(classText, server);
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
    public void DemonStrateThatGossipWorks() throws InterruptedException
    {
        System.out.println("The following Test will have printouts that demonstrate:" +
                "\n-Elections and Reelections Work" +
                "\n\t -a- First it will elect server 8" +
                "\n\t -a- Then it will kill server 8 and server 5 (the Leader and a Follower)" +
                "\n\t     so, when server 7 takes over, the quorum will be down 2 servers" +
                "\n\t -c- Then it will elect server 7" +
                "\n\t -d- Then it will kill 7, and elect server 6" +
                "\n\t --- All of this election Logic will be printed out and asserted (well not the last part, but the earlier stuff is enough proof)" +
                "\nThroughout all of this it will handle various client requests and print out and assert that it's working" +
                "\n\t --> For each request, I print out the whole journey of a client request, from the Client->Gateway->Leader->Follower and back." +
                "\n\t So it will print like this:" +
                "\n\t\t-i-    Which leader gets it" +
                "\n\t\t-ii-   Which which follower it sends it to" +
                "\n\t\t-iii-  The follower receiving it" +
                "\n\t\t-iv-   The follower doing the work" +
                "\n\t\t-v-    Which leader receives the completed work" +
                "\n\t\t-vi-   and subsequently, the client request will be fulfilled" +
                "\nAt the end, I demonstrate sending out a request to the cluster and then \"right away\" kill the leader" +
                "\n\tand if you look at my printouts, it will show that the follower gets the work from" +
                "\n\tone leader but responds to a different leader with the completed work" +
                "\n\nAt the end, it will print out the state of each server. See that the epoch, leader, and peerList are all updated correctly");
        Util2.resetPrinting();
        Util2.addClientWorkTrackingPrinting();
        Util2.addElectionCompletionPrintouts();
        testWithAClient();
    }

    @Test
    public void demonstrateThatGossipWorks() throws InterruptedException
    {
        System.out.println("This is the same test as before, but this time, there gossip will be shown");
        Util2.resetPrinting();
        Util2.addGossipPrintouts();
        Util2.addElectionCompletionPrintouts();
        testWithAClient();
    }
}
