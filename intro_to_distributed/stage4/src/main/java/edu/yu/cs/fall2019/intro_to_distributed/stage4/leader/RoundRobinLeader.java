/**
 * Thanks to https://stackoverflow.com/questions/10131377/socket-programming-multiple-client-to-one-server
 */
package edu.yu.cs.fall2019.intro_to_distributed.stage4.leader;

/**
 * Leader Logic:
 * [1] Setup a tcp Sender and Receiver
 * [2] Look in message queue
 * [a] If I got messages from the client
 * [i]   Look at the message
 * [ii]  Pass work the next FOLLOWER (in round robin fashion)
 * [b] If I got a message from a worker
 * [i] Look at the message
 * [ii] Return the result to the client
 */

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * [1] Setup connection with client <2 threads
 * [2] Setup connection with each FOLLOWER <-2 threads per FOLLOWER
 * [3] Take work from the leader and move it to a follower in a roundRobin fashion <-thread
 * [4] Take work from the clients and send it to the gateway <- thread
 */

public class RoundRobinLeader implements Runnable
{
    public final int CLIENT_PORT = 7090;
    private volatile boolean shutdown = false;
    private LinkedBlockingQueue<Message> messagesFromAllFollowers = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Message> messagesFromGateway = new LinkedBlockingQueue<>();
    private ConcurrentHashMap<Long, LinkedBlockingQueue<Message>> followerOutgoing = new ConcurrentHashMap<>();
    private final Map<Long, InetSocketAddress> peerIDtoAddress;

    private final int MY_PORT;
    private final String HOST = "localhost";
    public final int GATEWAY_PORT = 7070;

    //this is NOT REDUNDANT. This holds the set of NOT YET COMPLETED work for each server
    //in case the server goes down and we need to redo the work that that server never
    //sent back
    private final ConcurrentHashMap<Long, List<Message>> followersWork = new ConcurrentHashMap<>();

    private SocketHandler socketHandler;
    private RoundRobinFeeder roundRobinFeeder;
    private GatewayFeeder gatewayFeeder;

    public RoundRobinLeader(Map<Long, InetSocketAddress> peerIDtoAddress, int MY_PORT)
    {
        this.peerIDtoAddress = peerIDtoAddress;
        this.MY_PORT = MY_PORT;
    }

    public RoundRobinLeader(Map<Long, InetSocketAddress> peerIDtoAddress, int MY_PORT, LinkedBlockingQueue<Message> fromGateway)
    {
        this.peerIDtoAddress = peerIDtoAddress;
        this.MY_PORT = MY_PORT;
        this.messagesFromGateway.addAll(fromGateway);
    }

    public void shutdown()
    {
        Util2.printReplace("Shutting down round robin leader", 14);
        //todo - shutdown everything
        if(socketHandler != null) socketHandler.shutdown();
        if(roundRobinFeeder != null) roundRobinFeeder.shutdown();
        if(gatewayFeeder != null) gatewayFeeder.shutdown();
        this.shutdown = true;
    }

    @Override
    public void run()
    {
        // [1] Setup connection with client <2 threads
        // [2] Setup connection with each FOLLOWER <-2 threads per FOLLOWER
        setupSocketHandler();
        // [3] Take work from the leader and move it to a follower in a roundRobin fashion <-thread
        setupRoundRobinFeeder();
        // [4] Take work from the clients and send it to the gateway <- thread
        //setupGateWayFeeder();<-- this is too early, I need to wait until I get a connection from the gateway
        Util2.printReplace("Got the end of the [roundRobinLeader], so turning off ", 770);
    }

    /**
     * Create 2N threads, where N = number of Followers + gateway
     */
    private void setupSocketHandler()
    {
        socketHandler = new SocketHandler(MY_PORT,
                GATEWAY_PORT,
                HOST,
                messagesFromAllFollowers,
                messagesFromGateway,
                followerOutgoing,
                peerIDtoAddress,
                followersWork);
        new Thread(socketHandler).start();
    }

    /**
     * Take work from the leader and move it to a FOLLOWER in a round robin fashion
     */
    private void setupRoundRobinFeeder()
    {
        roundRobinFeeder = new RoundRobinFeeder(messagesFromGateway, followerOutgoing, followersWork);
        Util.startAsDaemon(roundRobinFeeder, "roundRobinFeeder");
    }

    ///////////////
    //  STAGE 4  //
    ///////////////
    public void killFollower(int port, long sid)
    {
        socketHandler.killFollower(port, sid);
    }
}
