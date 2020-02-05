package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketHandler implements Runnable
{
    private final int MY_PORT;
    private final String HOST;
    private final int GATEWAY_PORT = GateWay.GATEWAY_PORT;
    private final LinkedBlockingQueue<Message> messagesFromAllFollowers;
    private final LinkedBlockingQueue<Message> messagesFromGateway;
    private final Map<Long, LinkedBlockingQueue<Message>> followerOutgoingQueues;
    private final HashMap<Long, InetSocketAddress> peerIDtoAddress;
    private final Map<Long, List<Message>> followersWork;

    private HashMap<Long, TCPMessageReciever> receivers = new HashMap<>();
    private HashMap<Long, TCPMessageSender> senders = new HashMap<>();
    private ServerSocket serverSocket;
    private GatewayFeeder gatewayFeeder;
    private volatile boolean shutdown = false;

    public SocketHandler(int MY_PORT, int GATEWAY_PORT, String HOST, LinkedBlockingQueue<Message> messagesFromAllFollowers, LinkedBlockingQueue<Message> messagesFromGateway, Map<Long, LinkedBlockingQueue<Message>> followerOutgoing, HashMap<Long, InetSocketAddress> peerIDtoAddress, Map<Long, List<Message>> followersWork)
    {
        this.MY_PORT = MY_PORT;
        this.HOST = HOST;
        this.messagesFromAllFollowers = messagesFromAllFollowers;
        this.messagesFromGateway = messagesFromGateway;
        this.followerOutgoingQueues = followerOutgoing;
        this.peerIDtoAddress = peerIDtoAddress;
        this.followersWork = followersWork;
    }

    /**
     * Shutdown all the sockets and threads this class created
     */
    public void shutdown()
    {
        Util2.printReplace("Shutting down socket Handler");
        for (Long l : senders.keySet()) {
            Util2.printReplace("Shutting down sender/reciever port:" + l);
            senders.get(l).shutDown();
            receivers.get(l).shutDown();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            Util2.printReplace("Error closing the socketHandler", 804);
            e.printStackTrace();
        }
        gatewayFeeder.shutdown();
        shutdown = true;
    }

    @Override
    public void run()
    {
        /**
         * Stay awake to always setup new servers if some come up
         */
        serverSocket = null;
        Util2.printReplace("Starting SocketHandler", 805);
        try {
            serverSocket = new ServerSocket(MY_PORT);
        } catch (IOException e) {
            Util2.printReplace("Error establishing the socket for the leader, error = " + e, 804);
            e.printStackTrace();
        }
        while (!shutdown) {
            try {
                Socket socket = serverSocket.accept();
                //give the handler a queue as well
                TCPMessageReciever receiver;
                TCPMessageSender sender;
                long port = socket.getPort();
                LinkedBlockingQueue<Message> newOutgoingMessages = new LinkedBlockingQueue<>();
                if (port == GATEWAY_PORT) {
                    Util2.printReplace("I got a connection from the gateway! At port:" + port, 805);
                    sender = new TCPMessageSender(socket, newOutgoingMessages);
                    receiver = new TCPMessageReciever(socket, messagesFromGateway);
                    new Thread(sender).start();
                    new Thread(receiver).start();
                } else {//it's a FOLLOWER
                    sender = new TCPMessageSender(socket, newOutgoingMessages);
                    receiver = new TCPMessageReciever(socket, messagesFromAllFollowers);
                    new Thread(sender).start();
                    new Thread(receiver).start();
                }
                senders.put(port, sender);
                receivers.put(port, receiver);
                followerOutgoingQueues.put(port, newOutgoingMessages);
                //setup the gateway feeder now
                if(port == GATEWAY_PORT) setupGateWayFeeder();
            } catch (IOException e) {
                Util2.printReplace("Error in socketHandler " + e, 804);
                e.printStackTrace();
            }
        }
        Util2.printReplace("Got the end of the [SocketHandler], so turning off ", 770);
    }

    private void setupGateWayFeeder()
    {
        Util2.printReplace("Got a connection from the gateways so Setting up the feeder",805);
        gatewayFeeder = new GatewayFeeder(followersWork,
                messagesFromAllFollowers,
                GATEWAY_PORT,
                followerOutgoingQueues);
        new Thread(gatewayFeeder).start();
        Util2.printReplace("followerOutgoingQueues.get(" + GATEWAY_PORT+") = " +followerOutgoingQueues.get((long)GATEWAY_PORT),805);
        System.out.println(followerOutgoingQueues.keySet());
    }
}
