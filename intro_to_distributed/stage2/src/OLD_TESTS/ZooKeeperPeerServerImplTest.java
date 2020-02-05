import edu.yu.cs.fall2019.intro_to_distributed.Vote;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ZooKeeperPeerServerImplTest
{
    @Test
    //thanks @Judah for most of this code
    public void test8ServersFollow8()
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
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        }
        //wait for threads to start
        try
        {
            boolean moveOn = false;
            while (!moveOn) {
                Thread.sleep(5000);
                int count = 0;
                for (ZooKeeperPeerServer server : servers) if(server.getCurrentLeader() != null) count++;
                if(count >= (servers.size()/2+1)) moveOn = true;
            }
        }
        catch (Exception e)
        {
        }
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers)
        {
            Vote leader = server.getCurrentLeader();
            if (leader != null)
            {
                System.out.println("Server on port " + server.getMyAddress().getPort() + " whose ID is " + server.getId() +
                        " has the following ID as its leader: " + leader.getCandidateID() + " and its state is " + server.getPeerState().name());
                server.shutdown();
            }
        }
        for (ZooKeeperPeerServer server : servers){
            if(server.getId() == 8) assertEquals("The leader " + server.getId() + " isn't leading",
                    server.getPeerState(), ZooKeeperPeerServer.ServerState.LEADING);
            else {
                assertEquals("The Follower " + server.getId() + " is supposed to be the FOLLOWING state",
                        server.getPeerState(), ZooKeeperPeerServer.ServerState.FOLLOWING);
                assertEquals("The Follower " + server.getId() + " is supposed to be following server 8",
                        server.getCurrentLeader().getCandidateID(), 8);
            }
        }
    }

    @Test
    public void testAllHaveTheSameLeader()
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
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        }
        //wait for threads to start
        try
        {
            boolean moveOn = false;
            while (!moveOn) {
                Thread.sleep(5000);
                int count = 0;
                for (ZooKeeperPeerServer server : servers) if(server.getCurrentLeader() != null) count++;
                //if(count >= (servers.size()/2+1)) moveOn = true;
                if(count >= (servers.size())) moveOn = true;
            }
        }
        catch (Exception e)
        {
        }
        System.out.println("Server size = " + servers.size() + ",  quorum size = " + servers.get(0).getQuorumSize());
        long lead = -1;
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers)
        {
            Vote leader = server.getCurrentLeader();
            if (leader != null)
            {
                System.out.println("Server on port " + server.getMyAddress().getPort() + " whose ID is " + server.getId() +
                        " has the following ID as its leader: " + leader.getCandidateID() + " and its state is " + server.getPeerState().name());
                server.shutdown();
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
    }
    @Test
    public void testAllHaveTheSameLeaderAllowsLooking()
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
            HashMap<Long, InetSocketAddress> map = (HashMap<Long, InetSocketAddress>) peerIDtoAddress.clone();
            map.remove(entry.getKey());
            ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(entry.getValue().getPort(), 0, entry.getKey(), map);
            servers.add(server);
            new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        }
        //wait for threads to start
        try
        {
            boolean moveOn = false;
            while (!moveOn) {
                Thread.sleep(5000);
                int count = 0;
                for (ZooKeeperPeerServer server : servers) if(server.getCurrentLeader() != null) count++;
                if(count >= (servers.size()/2+1)) moveOn = true;
                //if(count >= (servers.size())) moveOn = true;
            }
        }
        catch (Exception e)
        {
        }
        System.out.println("Server size = " + servers.size() + ",  quorum size = " + servers.get(0).getQuorumSize());
        long lead = -1;
        //print out the leaders and shutdown
        for (ZooKeeperPeerServer server : servers)
        {
            Vote leader = server.getCurrentLeader();
            if (leader != null)
            {
                System.out.println("Server on port " + server.getMyAddress().getPort() + " whose ID is " + server.getId() +
                        " has the following ID as its leader: " + leader.getCandidateID() + " and its state is " + server.getPeerState().name());
                server.shutdown();
                lead = server.getCurrentLeader().getCandidateID();
            }
        }
        for (ZooKeeperPeerServer server : servers){
            if(server.getId() == lead) assertEquals("The leader " + server.getId() + " isn't leading",
                    server.getPeerState(), ZooKeeperPeerServer.ServerState.LEADING);
            else {
                if(server.getPeerState() == ZooKeeperPeerServer.ServerState.LOOKING){
                    System.out.println("Server " + server.getId() + " didn't finish for some reason");
                }
                else {
                    assertEquals("The Follower " + server.getId() + " is supposed to be the FOLLOWING state",
                            ZooKeeperPeerServer.ServerState.FOLLOWING, server.getPeerState());
                    assertEquals("The Follower " + server.getId() + " is supposed to be following server " + lead,
                            server.getCurrentLeader().getCandidateID(), lead);
                }
            }
        }
    }

    @Ignore
    @Test
    public void test10Times()
    {
        for(int i = 0; i < 10; i++) {
            System.out.println("<><><><><><><><><><><><><><>");
            System.out.println("<><>  Started test " + i + "  <><>");
            System.out.println("<><><><><><><><><><><><><><>");
            testAllHaveTheSameLeaderAllowsLooking();
            System.out.println("<><><><><><><><><><><><><><>");
            System.out.println("<><>  Ended test " + i + "  <><>");
            System.out.println("<><><><><><><><><><><><><><>");
        }
    }
}