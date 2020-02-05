package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

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

    public SimpleServerImpl(LinkedBlockingQueue<Message> incomingMessages, Map<Long, HttpExchange> exchanges, int leaderPort, String leaderHost)
    {
        this.incomingMessagesForLeader = incomingMessages;
        this.exchanges = exchanges;
        this.leaderPort = leaderPort;
        this.leaderHost = leaderHost;
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

}
