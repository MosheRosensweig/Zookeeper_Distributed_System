package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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
        Util2.printReplace("Shutting down message sender port:" + socket.getLocalPort(), 605);
        shutdown = true;
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
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(mesBytes.length);
                    out.write(mesBytes);
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
        Util2.printReplace("Got the end of the [TCP MessageSender " + socket.getLocalPort()+"], so turning off ", 770);
    }
}
