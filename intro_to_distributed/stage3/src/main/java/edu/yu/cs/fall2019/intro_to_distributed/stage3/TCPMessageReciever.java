package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPMessageReciever implements Runnable
{
    private Socket socket;
    private LinkedBlockingQueue<Message> incomingMessages;
    private volatile boolean shutdown = false;


    /**
     * When a TCPMessageReceiverSender starts it up
     */
    public TCPMessageReciever(Socket socket, LinkedBlockingQueue<Message> incomingMessages)
    {
        this.socket = socket;
        this.incomingMessages = incomingMessages;
    }

    public void shutDown()
    {
        Util2.printReplace("Shutting down message receiver port:" + socket.getLocalPort(), 705);
        shutdown = true;
    }

    @Override
    public void run()
    {
        Util2.printReplace("Receiver " + socket.getLocalPort() + " Got here1", 706);
        if (socket == null) throw new NullPointerException("Socket cannot be null");

        while (!shutdown) {
            try {
                socket.setSoTimeout(2000); // let it shutdown
                Util2.printReplace("Receiver " + socket.getLocalPort() + " waiting for a message", 706);


//                byte[] bytes = new byte[10000];
//                socket.getInputStream().read(bytes);
//                Message message = new Message(bytes);
                Message message = getNextMessage();
                incomingMessages.put(message);
                Util2.printReplace(">>>>Receiver<<<< " + socket.getLocalPort() + " got a message, contents = " + new String(message.getMessageContents()), 705);
            } catch (SocketTimeoutException e) {
                Util2.printReplace("Receiver " + socket.getLocalPort() + " timed out ", 706);
                continue;
            } catch (Exception e) {
                Util2.printReplace("Error in tcp receiver, local port =  " + socket.getLocalPort() +
                        ", other port = " + socket.getPort() + ", exception = " + e, 704);
                e.printStackTrace();
                System.exit(1);
            }
        }
        Util2.printReplace("Got the end of the [TCP MessageReciever " + socket.getLocalPort() + "], so turning off ", 770);
    }

    private Message getNextMessage() throws IOException
    {
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        int length = in.readInt();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int totalBytesRead = 0;
        int arraySize = length;
        while (arraySize > 0) {
            byte[] messageByte = new byte[arraySize];
            int currentBytesRead = in.read(messageByte);
            totalBytesRead = currentBytesRead + totalBytesRead;
            buffer.write(messageByte, 0, currentBytesRead);
            arraySize = length - totalBytesRead;
        }
        buffer.flush();
        Message message = new Message(buffer.toByteArray());
        return message;
    }
}
