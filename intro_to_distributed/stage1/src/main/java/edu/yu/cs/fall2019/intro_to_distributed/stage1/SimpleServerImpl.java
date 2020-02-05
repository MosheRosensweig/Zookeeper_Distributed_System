package edu.yu.cs.fall2019.intro_to_distributed.stage1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class SimpleServerImpl implements SimpleServer
{
    private HttpServer server;
    private int port = 8080;

    public SimpleServerImpl(int port) throws IllegalArgumentException, IOException
    {
        if(port < 1024) throw new IllegalArgumentException("Port number too low");
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/shutdown", new ShutdownHandler());
        server.createContext("/compileandrun", new CompAndRunHandler());
        server.setExecutor(null); // creates a default executor
    }

    private class ShutdownHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) throws IOException
        {
            String response = "Shutting down server...";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
            stop();

        }
    }

    private class CompAndRunHandler implements HttpHandler
    {
        @Override
        public void handle(HttpExchange t) throws IOException
        {
            Exception e = null;
            String response = null;
            try {
                response =new JavaRunnerImpl().compileAndRun(t.getRequestBody());
            } catch (Exception ex) {
               e = ex;
            }
            int responseCode = 200;
            if(e != null){
                responseCode = 400;
                response = e.getMessage();
            }
            //else if (!response.startsWith("System.err:\n[]")) responseCode = 400;
            JavaRunnerImpl.logTest("responseCode is " + responseCode, "server");
            t.sendResponseHeaders(responseCode, response.length());
            //t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
    }

    public void start()
    {
        server.start();
    }

    public void stop()
    {
        JavaRunnerImpl.logTest("Server is turning off.", "server");
        server.stop(0);
        JavaRunnerImpl.logTest("Server has been turned off.", "server");
    }


    public static void main(String[] args) throws Exception
    {
        SimpleServer simpleServer = new SimpleServerImpl(8000);
        simpleServer.start();
        JavaRunnerImpl.logTest("Terminating execution", "sever");
    }
}
