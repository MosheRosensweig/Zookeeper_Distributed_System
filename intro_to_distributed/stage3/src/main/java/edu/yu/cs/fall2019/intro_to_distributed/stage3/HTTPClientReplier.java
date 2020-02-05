package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import com.sun.net.httpserver.HttpExchange;
import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HTTPClientReplier implements Runnable
{
    private final Map<Long, HttpExchange> exchanges;
    private final LinkedBlockingQueue<Message> returnToSender;
    private volatile boolean shutdown = false;

    public HTTPClientReplier(Map<Long, HttpExchange> exchanges, LinkedBlockingQueue<Message> returnToSender)
    {
        this.exchanges = exchanges;
        this.returnToSender = returnToSender;
    }

    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public void run()
    {
        /**
         * Get a message from the LEADER
         * Look at that message's ReqID, and use that to get the HttpExchange
         * Return the response
         */
        while (!shutdown) {
            try {
                Message messageToSend = this.returnToSender.poll(2, TimeUnit.SECONDS);
                if(messageToSend == null){
                    Thread.yield();
                    continue;
                }
                long reqId = messageToSend.getRequestID();
                HttpExchange t = exchanges.remove(reqId);
                if(t != null) sendReply(t, messageToSend); //t = null for tests only
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Util2.printReplace("Got the end of the [HTTPClientReplier], so turning off", 770);
    }

    private void sendReply(HttpExchange t, Message message) throws IOException
    {
        String response = null;

        response = new String(message.getMessageContents());
        int responseCodeFromStr = Integer.parseInt(response.substring(0,3));
        String responseStr = response.substring(3);


//        int responseCode = 200;
//        if (response.startsWith("400")) {
//            responseCode = 400;
//            String newResponse = response.substring(response.indexOf("*") + 1);
//            response = newResponse;
//        }
        //else if (!response.startsWith("System.err:\n[]")) responseCode = 400;
        //JavaRunnerImpl.logTest("responseCode is " + responseCode, "server");
//        t.sendResponseHeaders(responseCode, response.length());

        Util2.printReplace("Replier responseCode:" + responseCodeFromStr + ", content = " + responseStr, 770);
        t.sendResponseHeaders(responseCodeFromStr, responseStr.length());
        OutputStream os = t.getResponseBody();
        os.write(responseStr.getBytes());
        os.close();
        t.close();
    }
}
