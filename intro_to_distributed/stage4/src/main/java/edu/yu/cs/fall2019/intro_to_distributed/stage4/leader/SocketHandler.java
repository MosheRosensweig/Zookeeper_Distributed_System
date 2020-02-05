package edu.yu.cs.fall2019.intro_to_distributed.stage4.leader;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp.TCPMessageReciever;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp.TCPMessageSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketHandler implements Runnable
{
    private final int MY_PORT;
    private final String HOST;
    private final int GATEWAY_PORT = GateWay.GATEWAY_PORT;
    private final LinkedBlockingQueue<Message> messagesFromAllFollowers;
    private final LinkedBlockingQueue<Message> messagesFromGateway;
    private final Map<Long, LinkedBlockingQueue<Message>> followerOutgoingQueues;
    private final Map<Long, InetSocketAddress> peerIDtoAddress;
    private final Map<Long, List<Message>> followersWork;

    private Map<Long, TCPMessageReciever> receivers = new ConcurrentHashMap<>();
    private Map<Long, TCPMessageSender> senders = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private GatewayFeeder gatewayFeeder;
    private volatile boolean shutdown = false;

    public SocketHandler(int MY_PORT, int GATEWAY_PORT, String HOST, LinkedBlockingQueue<Message> messagesFromAllFollowers, LinkedBlockingQueue<Message> messagesFromGateway, Map<Long, LinkedBlockingQueue<Message>> followerOutgoing, Map<Long, InetSocketAddress> peerIDtoAddress, Map<Long, List<Message>> followersWork)
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
        Util2.printReplace("Shutting down socket Handler", 8);
        for (Long l : senders.keySet()) {
            Util2.printReplace("Shutting down sender/reciever port:" + l, 8);
            senders.get(l).shutDown();
            receivers.get(l).shutDown();
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            Util2.printReplace("Error closing the socketHandler", 804);
            e.printStackTrace();
        }
        if (gatewayFeeder != null) gatewayFeeder.shutdown();
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
                if (port == GATEWAY_PORT) setupGateWayFeeder();
            } catch (IOException e) {
                Util2.printReplace("Error in socketHandler " + e, 804);
                //e.printStackTrace();
            }
        }
        Util2.printReplace("Got the end of the [SocketHandler], so turning off ", 770);
    }

    private void setupGateWayFeeder()
    {
        Util2.printReplace("Got a connection from the gateways so Setting up the feeder", 805);
        gatewayFeeder = new GatewayFeeder(followersWork,
                messagesFromAllFollowers,
                GATEWAY_PORT,
                followerOutgoingQueues);
        new Thread(gatewayFeeder).start();
        Util2.printReplace("followerOutgoingQueues.get(" + GATEWAY_PORT + ") = " + followerOutgoingQueues.get((long) GATEWAY_PORT), 805);
        //System.out.println(followerOutgoingQueues.keySet());
    }

    ///////////////
    //  STAGE 4  //
    ///////////////
    public void killFollower(int port, long sid)
    {
        //I did 2 stupid things by accident:
        //[1]   I used ports as the ids, even though sids were available - but there was a reason for this
        //      but I have since then figured out how to get around it
        //[2]   I sometimes consider ports to be a long, when they are really ints - stupid
        long port_as_long = (long) port;
        //[0] Make sure this server connected to me!
        if (!followerOutgoingQueues.containsKey(port_as_long)) {
            Util2.printReplace("Leader attempted to delete server:" + sid + ", at port:" + port + " but that server doesn't have a connection established", 16);
            return;
        }
        else {
            //[1] break the sockets
            ArrayList<Exception> exceptions = new ArrayList<>();
            try {//I'm not expecting exceptions, but who knows
                senders.get(port_as_long).shutDown();
            } catch (Exception e) {
                exceptions.add(e);
            }
            try {//I'm not expecting exceptions, but who knows
                receivers.get(port_as_long).shutDown();
            } catch (Exception e) {
                exceptions.add(e);
            }
            //[2] Erase them
            senders.remove(port_as_long);
            receivers.remove(port_as_long);
            //[3] Work that I was going to send to that follower, put it back in the main queue
            LinkedBlockingQueue<Message> outGoingQueue = followerOutgoingQueues.get(port_as_long);
            messagesFromGateway.addAll(outGoingQueue);
            outGoingQueue.clear();   // <-- I think this is extra, but why not
            //[4] Work that I already sent to the follower, put it back in the main queue
            List<Message> messages = followersWork.get(port_as_long);
            messagesFromGateway.addAll(followersWork.get(port_as_long));
            followersWork.get(port_as_long).clear();            // <-- I think this is extra, but why not
            //[5] Erase them too - so they won't be in the roundRobinLeader -- did that above
            followerOutgoingQueues.remove(port_as_long);
            followersWork.remove(port_as_long);
            for (Exception e : exceptions) Util2.printReplace("Kill a FOLLOWER Error: " + e.getMessage(), 804);
        }
        //removed it from the peerAddress at the top of the stack
    }
}
