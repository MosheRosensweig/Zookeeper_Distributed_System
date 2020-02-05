package edu.yu.cs.fall2019.intro_to_distributed.stage2;

import edu.yu.cs.fall2019.intro_to_distributed.Vote;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Stage2Test
{
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
}
