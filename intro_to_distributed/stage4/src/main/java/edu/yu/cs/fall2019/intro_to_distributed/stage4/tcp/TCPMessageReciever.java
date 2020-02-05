package edu.yu.cs.fall2019.intro_to_distributed.stage4.tcp;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.io.*;
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
        Util2.printReplace("Shutting down message receiver port:" + socket.getLocalPort(), 707);
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
        Util2.printReplace("Receiver " + socket.getLocalPort() + " Got here1", 706);
        if (socket == null) throw new NullPointerException("Socket cannot be null");

        while (!shutdown) {
            try {
                socket.setSoTimeout(2000); // let it shutdown
                Util2.printReplace("Receiver " + socket.getLocalPort() + " waiting for a message", 706);
//                byte[] bytes = new byte[10000];
//                socket.getInputStream().read(bytes);
//                Message message = new Message(bytes);

                //Message message = getNextMessage(); //<-- stage3 way
                Message message = getNextMessage2();
                if (socket.getPort() == GateWay.GATEWAY_PORT && message != null)
                    Util2.printReplace("TCPReceiver for Leader at port:" + socket.getLocalPort() + ", got message with request id = "
                            + message.getRequestID() + " from the gateway", 13_01);
                if (message == null) continue;
                incomingMessages.put(message);
                Util2.printReplace(">>>>Receiver<<<< " + socket.getLocalPort() + " got a message, contents = " + new String(message.getMessageContents()), 705);
            } catch (SocketTimeoutException e) {
                Util2.printReplace("Receiver " + socket.getLocalPort() + " timed out ", 706);
                continue;
            } catch (Exception e) {
                if (socket.isClosed()) {
                    shutDown();
                } else {
                    Util2.printReplace("Error in tcp receiver, local port =  " + socket.getLocalPort() +
                            ", other port = " + socket.getPort() + ", exception = " + e, 704);
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        Util2.printReplace("Got the end of the [tcp MessageReceiver " + socket.getLocalPort() + "], so turning off ", 770);
    }

    private Message getNextMessage() throws IOException
    {
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        try {
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
        } catch (EOFException e) {
            shutDown();
            return null;
        }
    }

    private Message getNextMessage2() throws IOException
    {
        try {
            DataInputStream dIn = new DataInputStream(socket.getInputStream());
            int length = dIn.readInt();                    // read length of incoming message
            if (length > 0) {
                byte[] message = new byte[length];
                dIn.readFully(message, 0, message.length); // read the message
                Message messageRes = new Message(message);
                return messageRes;
            }
            return null;
        } catch (EOFException e) {
            shutDown();
            return null;
        }
    }
}
