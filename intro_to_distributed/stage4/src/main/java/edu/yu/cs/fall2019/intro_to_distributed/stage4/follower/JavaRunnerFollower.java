package edu.yu.cs.fall2019.intro_to_distributed.stage4.follower;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunnerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp.TCPMessageReciever;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp.TCPMessageSender;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * [1] Create a socket that reaches out to the LEADER
 * [2] Create a sender and receiver with that socket
 * [3] in this main thread, do the work
 */
public class JavaRunnerFollower implements Runnable
{
    private final int LEADER_PORT;
    private final String HOST;
    private final String MY_HOST = "localhost";
    private final int MY_PORT;

    private volatile boolean shutdown = false;
    private TCPMessageSender sender;
    private TCPMessageReciever receiver;
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
        if(sender != null) sender.shutDown();
        if(receiver != null) receiver.shutDown();
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
                    Util2.printReplace("Follower:"+MY_PORT+" successfully created a TCP connection to the leader", 17);
                }catch (ConnectException e){
                    Util2.printReplace("Follower:"+MY_PORT+" started too early, wait for the leader to turn on", 804);
                    Thread.yield();
                }catch (BindException e){
                    Util2.printReplace("Follower:"+MY_PORT+" got a bind error, wait a bit", 804);
                    Thread.sleep(1_000);
                }
            }
            sender = new TCPMessageSender(socket, outgoingMessages);
            receiver = new TCPMessageReciever(socket, incomingMessages);
            new Thread(sender).start();
            new Thread(receiver).start();
            doWork();
            Util2.printReplace("Got the end of the [JavaRunnerFollower], so turning off ", 770);
        } catch (IOException e) {
            Util2.printError(e, MY_PORT, 804);
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
            Util2.printReplace("Got a message from LEADER at port:"+ messageToSend.getReceiverPort()
            + " with message request id = "+messageToSend.getRequestID()
                    + " at FOLLOWER port:"+MY_PORT, 13_01);
            String clientResponse = doClientWork(messageToSend);
            Util2.printReplace("Finished doing Work for message with request id = "+ messageToSend.getRequestID(), 13_01);
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

    public void deleteOldLeader()
    {
        ArrayList<Exception> exceptions = new ArrayList<>();
        try{
            if(sender != null) sender.shutDown();
        }catch (Exception e){
            exceptions.add(e);
        }
        try{
            if(receiver != null) receiver.shutDown();
        }catch (Exception e){
            exceptions.add(e);
        }
        sender = null;
        receiver = null;
        //for(Exception e : exceptions) Util2.printReplace("Server:" + MY_PORT+ " deleteOldLeader error: " + e.getMessage(), 804);
        for(Exception e : exceptions) Util2.printError(e, MY_PORT, 804); // for now just print out the errors
    }

    /**
     * When a follower becomes a leader, let it run down it's queue
     * of work. Once it's empty, then we can transition over to the leader
     *
     * Note: This isn't perfect, there's still a chance that the work queue will
     * be empty and then we use this method to determine that and as a result shutdown the
     * jrf before that work is completed. I could add booleans before/after working, but for now,
     * leave it be.
     * @return
     */
    public boolean stillHaveWorkToDo()
    {
        return !incomingMessages.isEmpty();
    }
    public LinkedBlockingQueue<Message> getOutgoingMessages()
    {
        return outgoingMessages;
    }
}
