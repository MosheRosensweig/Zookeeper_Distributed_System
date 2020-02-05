package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;

import com.sun.net.httpserver.HttpExchange;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * [1] Setup an HTTP server
 * [2] Create a connection to the LEADER (once the election is over - this should be redoable for new elections)
 * [3] Have the two talk to each other
 * <p>
 * - More specifically, have a compileAndRunHandler, that takes in messages, and queues them into a map with a
 * unique request id. More detailed, map the HTTPEXCHANGES to ids, and turn the httpExchange into a message for
 * the message sender
 * - Have a sender send the messages
 * - Have a receiver receive the messages
 * - Have a responder respond to the messages
 */
public class GateWay implements Runnable
{
    public static final int GATEWAY_PORT = 7090;//internal port (different than GATEWAY_CLIENT_PORT, which is public)
    public static final String GATEWAY_HOST = "localhost";
    private Socket socket;
    private LinkedBlockingQueue<Message> outgoingWorkToLeader = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Message> incomingCOMPLETEDWork = new LinkedBlockingQueue<>();
    private Map<Long, HttpExchange> exchanges = new ConcurrentHashMap<>();
    private int leaderPort;
    private String leaderHost;

    private SimpleServerImpl simpleServer;
    private TCPMessageReciever receiver;
    private TCPMessageSender sender;
    private HTTPClientReplier replier;

    private volatile boolean shutdown = false;

    public void shutdown()
    {
        //todo - shutdown everything
        receiver.shutDown();
        sender.shutDown();
        simpleServer.shutdown();
        replier.shutdown();
        shutdown = true;
        Util2.printReplace("Shutting Down Gateway", 770);
    }

    @Override
    public void run()
    {
        //in stage4 this will happen in election logic I think
        setupClientHandler();
        connectToLeader();
        setupReplier();
    }

    public void setLeaderPort(int leaderPort)
    {
        this.leaderPort = leaderPort;
    }

    public void setLeaderHost(String host)
    {
        this.leaderHost = host;
    }

    public void setNewLeader(int leaderPort, String  leaderHost)
    {
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
    }

    /**
     * Create a socket to the LEADER
     * then create a sender and receiver for it
     */
    private void connectToLeader()
    {
        socket = null;
        try {
            while (socket == null){
                try {
                    socket = new Socket(leaderHost, leaderPort, InetAddress.getByName(GATEWAY_HOST), GATEWAY_PORT);
                }catch (ConnectException e){
                    Util2.printReplace("GATEWAY started too early, wait for the leader to turn on", 13_04);
                    Thread.yield();
                }

            }
            sender = new TCPMessageSender(socket, outgoingWorkToLeader);
            receiver = new TCPMessageReciever(socket, incomingCOMPLETEDWork);
            new Thread(sender).start();
            new Thread(receiver).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stage 4 stuff
     * @param leaderPort
     * @param leaderHost
     * @return
     */
    public boolean connectToNewLeader(int leaderPort, String leaderHost)
    {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
        if(sender != null) sender.shutDown();
        if(receiver != null) receiver.shutDown();
        connectToLeader();
        simpleServer.setNewLeader(leaderPort, leaderHost);
        return true;
    }

    private void setupReplier()
    {
        replier = new HTTPClientReplier(exchanges, incomingCOMPLETEDWork);
        new Thread(replier).start();
    }

    private void setupClientHandler()
    {
        simpleServer = new SimpleServerImpl(outgoingWorkToLeader, exchanges, leaderPort, leaderHost);
        new Thread(simpleServer).start();
    }

}
