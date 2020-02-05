package edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TCPMessageSender implements Runnable
{
    private Socket socket;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private volatile boolean shutdown = false;


    public TCPMessageSender(Socket socket, LinkedBlockingQueue<Message> outgoingMessages)
    {
        this.socket = socket;
        this.outgoingMessages = outgoingMessages;
    }

    public void shutDown()
    {
        Util2.printReplace("Shutting down message sender port:" + socket.getLocalPort(), 607);
        shutdown = true;
        try {
            socket.close();
        } catch (IOException e) {
            Util2.printError(e, socket.getLocalPort(), 707);
        }
    }

    @Override
    public void run()
    {
        //a socket has already been established
        Util2.printReplace("Sender "+ socket.getLocalPort() +" got here1", 606);
        if(socket == null) throw new NullPointerException("Socket cannot be null");
        while (!shutdown){
            try {
                Util2.printReplace("Sender "+ socket.getLocalPort() +" got here2", 606);
                Message messageToSend = this.outgoingMessages.poll(2, TimeUnit.SECONDS);
                if(messageToSend != null){
                    byte[] mesBytes = messageToSend.getNetworkPayload();

                    try {
                        DataOutputStream out = new DataOutputStream(socket.getOutputStream());// <-- moving this into the try has been weird
                        if(socket.getLocalPort() == GateWay.GATEWAY_PORT)
                            Util2.printReplace("Gateway TCPSender sending message with id "+messageToSend.getRequestID()
                                    +" to leader at port:"+socket.getPort(), 13_01);
                        out.writeInt(mesBytes.length);
                        out.write(mesBytes);
                    }catch (Exception e){
                        //the socket was broken on the other end
                        //add the message back
                        Util2.printReplace("Attempting to send message id:"+messageToSend.getRequestID() +
                                ", but instead wait for a new connection", 13_01);
                        this.outgoingMessages.offer(messageToSend, 2, TimeUnit.SECONDS);
                        shutDown();
                        continue;
                    }
                    //Util2.printReplace("sender port:"+ socket.getLocalPort() +", sending message of size:" + mesBytes.length, 770);
                    Util2.printReplace("----Sender---- "+ socket.getLocalPort() +" wrote something to port:"+socket.getPort()+", contents = " + new String(messageToSend.getMessageContents()), 605);
                }
                else Util2.printReplace("Sender "+ socket.getLocalPort() +" didn't write anything", 605);
            } catch (Exception e) {
                Util2.printReplace("Error in tcp sender, local port =  " + socket.getLocalPort() +
                        ", other port = " + socket.getPort() + ", exception = " + e, 604);
                e.printStackTrace();
                System.exit(1);
            }
        }
        Util2.printReplace("Got the end of the [tcp MessageSender " + socket.getLocalPort()+"], so turning off ", 770);
    }
}
