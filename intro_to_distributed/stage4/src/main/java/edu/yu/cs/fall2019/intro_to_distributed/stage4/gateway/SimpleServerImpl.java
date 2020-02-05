package edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class SimpleServerImpl implements Runnable
{
    private HttpServer server;
    private final int GATEWAY_CLIENT_PORT = 7095; //public port
    private final LinkedBlockingQueue<Message> incomingMessagesForLeader;
    private final Map<Long, HttpExchange> exchanges;
    private long requestID = 0;
    //leader details needed for sending the messages
    private int leaderPort;
    private String leaderHost;

    private volatile boolean shutdown = false;

    //////////////
    // TESTING  //
    //////////////
    private ZooKeeperPeerServerImpl parent;
    public SimpleServerImpl(LinkedBlockingQueue<Message> incomingMessages, Map<Long, HttpExchange> exchanges, int leaderPort, String leaderHost)
    {
        this.incomingMessagesForLeader = incomingMessages;
        this.exchanges = exchanges;
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
    }

    public SimpleServerImpl(LinkedBlockingQueue<Message> incomingMessages, Map<Long, HttpExchange> exchanges, int leaderPort, String leaderHost, ZooKeeperPeerServerImpl server)
    {
        this.incomingMessagesForLeader = incomingMessages;
        this.exchanges = exchanges;
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
        parent = server;
    }

    public void shutdown()
    {
        shutdown = true;
        stop();
    }
    @Override
    public void run()
    {
        Util2.printReplace("Simple Server has been started!", 11_05);
        try {
            server = HttpServer.create(new InetSocketAddress(GATEWAY_CLIENT_PORT), 0);
            server.createContext("/shutdown", new ShutdownHandler());
            server.createContext("/compileandrun", new CompAndRunHandler());
            server.createContext("/getLeaderAndClusterInfo", new LeaderHandler());
            server.setExecutor(null); // creates a default executor
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
        Util2.printReplace("Simple Server has been started2!", 11_05);
    }

    private class ShutdownHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) throws IOException
        {
            while (!shutdown) {
                String response = "Shutting down server...";
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                t.close();
                stop();
            }
            Util2.printReplace("Got the end of the [SimpleServerImpl], so turning off", 11_05);
        }
    }

    private class LeaderHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) throws IOException
        {
            while (!shutdown) {
                String response = generateClusterData();
                int code = (response.isEmpty()) ? 400 : 200;
                if(code == 400) response = "Still in an election";
                //System.out.println(response);
                t.sendResponseHeaders(code, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
                t.close();
            }
        }
    }

    private class CompAndRunHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) throws IOException
        {
            //take the code and
            long reqID = requestID++;
            Util2.printReplace("Got a request, reqId:"+reqID, 11_05);
            exchanges.put(reqID, t);
            String work = JavaRunnerImpl.streamToString(t.getRequestBody());
            Message message = new Message(Message.MessageType.WORK,
                    work.getBytes(),
                    GateWay.GATEWAY_HOST,
                    GateWay.GATEWAY_PORT,
                    leaderHost,
                    leaderPort,
                    reqID);
            try {
                incomingMessagesForLeader.put(message);
                Util2.printReplace("Gateway received message and gave it request id = " + message.getRequestID() + " to send to the leader at port:"+leaderPort, 13_01);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void start()
    {
        server.start();
    }
    public void setLeaderPort(int leaderPort)
    {
        this.leaderPort = leaderPort;
    }
    public void setLeaderHost(String leaderHost)
    {
        this.leaderHost = leaderHost;
    }
    public void setNewLeader(int leaderPort, String leaderHost)
    {
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
    }

    public void stop()
    {
        Util2.printReplace("Server is turning off.", 11_05);
        server.stop(0);
        Util2.printReplace("Server has been turned off.", 11_05);
    }


    private String generateClusterData()
    {
        if(parent.getPeerState() == ZooKeeperPeerServer.ServerState.OBSERVING_LOOKING) return "";
        StringBuilder sb = new StringBuilder();
        long leaderSid = parent.getCurrentLeader().getCandidateID();
        sb.append("Leader is sid:"+leaderSid);
        for(long sid : parent.getPeerIDtoAddress().keySet()){
            if(sid == leaderSid) sb.append("\n\tServer "+ sid + " is LEADING");
            else if(parent.getPeerIDtoAddress().get(sid).getPort() == GateWay.GATEWAY_PORT) sb.append("\n\tServer "+ sid + " is OBSERVING");
            else sb.append("\n\tServer "+ sid + " is FOLLOWING");
        }
        sb.append("\n\tServer " + parent.getId() + " is OBSERVING");
        sb.append("\n");
        return sb.toString();
    }
}
