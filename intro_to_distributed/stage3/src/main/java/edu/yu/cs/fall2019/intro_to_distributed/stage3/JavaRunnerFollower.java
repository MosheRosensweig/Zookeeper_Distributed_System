package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * [1] Create a socket that reaches out to the LEADER
 * [2] Create a sender and receiver with that socket
 * [3] in this main thread, do the work
 *
 *
 */
public class JavaRunnerFollower implements Runnable
{
    private final int LEADER_PORT;
    private final String HOST;
    private final String MY_HOST = "localhost";
    private final int MY_PORT;

    private volatile boolean shutdown = false;
    private TCPMessageSender sender;
    private TCPMessageReciever reciever;
    private LinkedBlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<Message> incomingMessages = new LinkedBlockingQueue<>();

    public JavaRunnerFollower(int LEADER_PORT, String host, int MY_PORT)
    {
        this.LEADER_PORT = LEADER_PORT;
        this.HOST = host;
        this.MY_PORT = MY_PORT;
    }

    public void shutdown()
    {
        sender.shutDown();
        reciever.shutDown();
        shutdown = true;
    }

    @Override
    public void run()
    {
        Socket socket = null;
        try {
            Util2.printReplace("Attempting to connect; host:" + HOST+", leaderPort:"+LEADER_PORT+", my_host:"+MY_HOST+", myport:"+MY_PORT, 770);
            while (socket == null) {
                try {
                    socket = new Socket(HOST, LEADER_PORT, InetAddress.getByName(MY_HOST), MY_PORT);
                }catch (ConnectException e){
                    Util2.printReplace("Follower:"+MY_PORT+" started too early, wait for the leader to turn on", 804);
                    Thread.yield();
                }
            }
            sender = new TCPMessageSender(socket, outgoingMessages);
            reciever = new TCPMessageReciever(socket, incomingMessages);
            new Thread(sender).start();
            new Thread(reciever).start();
            doWork();
            Util2.printReplace("Got the end of the [JavaRunnerFollower], so turning off ", 770);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void doWork() throws InterruptedException
    {
        while (!shutdown){
            Message messageToSend = this.incomingMessages.poll(2, TimeUnit.SECONDS);
            if(messageToSend == null){
                Thread.yield();
                continue;
            }
            //todo - replace this with the actual javarunner stuff
            String temporary = new String(messageToSend.getMessageContents());
            //String clientResponse = "Client response to " + temporary; //for testing
            String clientResponse = doClientWork(messageToSend);
            Message send = new Message(Message.MessageType.COMPLETED_WORK,
                    clientResponse.getBytes(),
                    MY_HOST,
                    MY_PORT,
                    HOST,
                    LEADER_PORT,
                    messageToSend.getRequestID());
            outgoingMessages.put(send);
        }
    }

    private String doClientWork(Message message)
    {
        String contents = new String(message.getMessageContents());

        Exception e = null;
        String response = null;
        try {
            response = new JavaRunnerImpl().compileAndRun(contents);
        } catch (Exception ex) {
            e = ex;
        }
        int responseCode = 200;
        if(e != null){
            responseCode = 400;
            response = e.getMessage();
        }
        return responseCode + response;
    }
}
