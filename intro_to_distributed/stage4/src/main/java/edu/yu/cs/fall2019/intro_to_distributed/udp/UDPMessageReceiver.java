package edu.yu.cs.fall2019.intro_to_distributed.udp;

import edu.yu.cs.fall2019.intro_to_distributed.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPMessageReceiver implements Runnable
{
    private static final int MAXLENGTH = 4096;
    private final InetSocketAddress myAddress;
    private final int myPort;
    private LinkedBlockingQueue<Message> incomingMessages;
    private LinkedBlockingQueue<Message> incomingGossipMessages;
    private volatile boolean shutdown = false;

    public UDPMessageReceiver(LinkedBlockingQueue<Message> incomingMessages, InetSocketAddress myAddress,int myPort, LinkedBlockingQueue<Message> incomingGossipMessages)
    {
        this.incomingMessages = incomingMessages;
        this.myAddress = myAddress;
        this.myPort = myPort;
        this.incomingGossipMessages = incomingGossipMessages;
    }

    public void shutdown()
    {
        this.shutdown = true;
    }

    @Override
    public void run()
    {
        //create the socket
        DatagramSocket socket = null;
        try
        {
            socket = new DatagramSocket(this.myAddress);
            socket.setSoTimeout(3000);
        }
        catch(Exception e)
        {
            System.err.println("failed to create receiving socket");
            e.printStackTrace();
        }
        //loop
        while (!this.shutdown)
        {
            try
            {
                DatagramPacket packet = new DatagramPacket(new byte[MAXLENGTH], MAXLENGTH);
                socket.receive(packet); // Receive packet from a client
                Message message = new Message(packet.getData());
                Message.MessageType type = message.getMessageType();
                if(type == Message.MessageType.ELECTION) incomingMessages.put(message);
                else if(type == Message.MessageType.HEARTBEAT || type == Message.MessageType.GOSSIP) incomingGossipMessages.put(message);
                else throw new IllegalStateException("Server at port:" + socket.getLocalPort()+" got a non-election, non-gossip, non-heartbeat UDP message");

            }
            catch(SocketTimeoutException ste)
            {
            }
            catch (Exception e)
            {
                if (!this.shutdown)
                {
                    e.printStackTrace();
                }
            }
        }
        //cleanup
        if(socket != null)
        {
            socket.close();
        }
    }
}