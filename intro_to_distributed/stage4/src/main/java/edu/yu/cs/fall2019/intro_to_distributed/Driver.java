package edu.yu.cs.fall2019.intro_to_distributed;

import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class Driver
{
    public static void main(String[] args)
    {
        //Util2.addClientWorkTrackingPrinting();

        long id = Long.parseLong(args[0]);

        Util2.createGossipFile(id);

        ConcurrentHashMap<Long, InetSocketAddress> peerIDtoAddress = new ConcurrentHashMap<>();
        peerIDtoAddress.put(1L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(2L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(3L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(4L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(5L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(6L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(7L, new InetSocketAddress("localhost", ZooKeeperPeerServerImpl.generateNewInetAddress()));
        peerIDtoAddress.put(8L, new InetSocketAddress("localhost", GateWay.GATEWAY_PORT));

        int port = peerIDtoAddress.get(id).getPort();
        peerIDtoAddress.remove(id);
        ZooKeeperPeerServer server = new ZooKeeperPeerServerImpl(port, 0, id, peerIDtoAddress);
        new Thread(server, "Server on port " + server.getMyAddress().getPort()).start();
        //todo - remove these printouts

        System.out.println("Server "+id+" Started");
    }
}
