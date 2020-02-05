package edu.yu.cs.fall2019.intro_to_distributed.util;

import edu.yu.cs.fall2019.intro_to_distributed.JavaRunner;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.election.ElectionNotification;
import edu.yu.cs.fall2019.intro_to_distributed.election.Vote;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Util2
{
    /**
     * 0 : Always Off
     * 1 : Always On
     * 2 : Election Debugging
     * 201 : Election show votes
     * 3 : Election Complete
     * 4 : Server functionality
     * 5 : LEADER server
     * 6 : tcp Sender
     * 604 : tcp Sender Error
     * 605 : tcp Sender Debug
     * 606 : tcp Sender deubg level 2
     * 7 : tcp Receiver
     * 704 : tcp Receiver Error
     * 705 : tcp Receiver Debug
     * 706 : tcp Receiver deubg level 2
     * 8 : Socket Handler
     * 804 : Socket Handler Error
     * 805 : Socket Handler Debug
     * 9
     * 905 : Gateway Feeder Debug
     * 909 : Gateway Feeder outgoing
     * 10
     * 10_04 :
     * 11 : Simple Server
     * 11_05 : Simple Server Debug
     * 12 : ZooKeeperServer
     * 12_05 : ZooKeeperServer Debug
     * 13 : Gateway
     * 13_01 : Round trip client message
     * 14 : Leader
     * 14_01 : RoundRobinFeeder outgoing
     * 15 : Gossip
     * 16 : Fault tolerance
     * 17 : Follower
     * 17_01 : JavaRunnerFollower TCP successfully connected to the leader
     * <p>
     * //stage4
     * 4_1 election responses
     */
    //private static int[] printingLevelsThatAreOn = {1,4_1,16, 12_05,804}; <-- Fault Tolerance Test - demonstrate successful TCP connection to new leader
    //private static int[] printingLevelsThatAreOn = {1,4_1,16,15}; <-- Fault Tolerance Test configuration with Gossip
    //private static int[] printingLevelsThatAreOn = {1,3, 13_01,16}; <-- Demonstrate that the client call works, and even between two leaders
    private static int[] printingLevelsThatAreOn = {1};
    private static ArrayList<Integer> printingLevelsThatAreOnList = new ArrayList<>();

    public static void resetPrinting()
    {
        synchronized (printingLevelsThatAreOn) {
            printingLevelsThatAreOn = new int[]{1};
            printingLevelsThatAreOnList.clear();
            printingLevelsThatAreOnList.add(1);
        }
    }

    /**
     * Adds the levels that will print out the journey of work from a client to the gateway
     * to the leader to the follower and then back
     */
    public static void addClientWorkTrackingPrinting()
    {
        synchronized (printingLevelsThatAreOn) {
            printingLevelsThatAreOnList.add(13_01);
            int[] temp = new int[printingLevelsThatAreOnList.size()];
            for (int i = 0; i < temp.length; i++) temp[i] = printingLevelsThatAreOnList.get(i);
            printingLevelsThatAreOn = temp;
        }
    }

    public static void addElectionCompletionPrintouts()
    {
        synchronized (printingLevelsThatAreOn) {
            printingLevelsThatAreOnList.add(3);
            int[] temp = new int[printingLevelsThatAreOnList.size()];
            for (int i = 0; i < temp.length; i++) temp[i] = printingLevelsThatAreOnList.get(i);
            printingLevelsThatAreOn = temp;
        }
    }

    public static void addFaultTolerancePrintouts()
    {
        synchronized (printingLevelsThatAreOn) {
            printingLevelsThatAreOnList.add(16);
            int[] temp = new int[printingLevelsThatAreOnList.size()];
            for (int i = 0; i < temp.length; i++) temp[i] = printingLevelsThatAreOnList.get(i);
            printingLevelsThatAreOn = temp;
        }
    }

    public static void addGossipPrintouts()
    {
        synchronized (printingLevelsThatAreOn) {
            printingLevelsThatAreOnList.add(15);
            int[] temp = new int[printingLevelsThatAreOnList.size()];
            for (int i = 0; i < temp.length; i++) temp[i] = printingLevelsThatAreOnList.get(i);
            printingLevelsThatAreOn = temp;
        }
    }

    ///https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    public static byte[] electionNotificationToByteArr(ElectionNotification en)
    {
        byte[] contents = null;
        String asStr = en.leader + "," + en.state + "," + en.sid + "," + en.peerEpoch;
        contents = asStr.getBytes();
        return contents;
    }

    public static ElectionNotification byteArrToElectionNotification(byte[] b)
    {
        String[] fields = new String(b).split(",");
        long leader = Long.parseLong(fields[0]);
        ZooKeeperPeerServer.ServerState state = stringToState(fields[1]);
        long sid = Long.parseLong(fields[2]);
        long peerEpoch = Long.parseLong(fields[3]);
        return new ElectionNotification(leader, state, sid, peerEpoch);//todo - implement
    }

    private static void printReplace(String s)
    {
        synchronized (JavaRunner.class) {
            System.out.println(s);
        }
    }

    public static void printReplace(String s, int level)
    {
        boolean print = false;
        for (int i : printingLevelsThatAreOn)
            if (level == i) {
                print = true;
                break;
            }
        if (print) printReplace(s);
    }

    public static Vote electionNotificationToVote(ElectionNotification en)
    {
        return new Vote(en.leader, en.peerEpoch, en.state);
    }

    public static ZooKeeperPeerServer.ServerState stringToState(String str)
    {
        ZooKeeperPeerServer.ServerState state = null;
        str = str.toLowerCase();
        if (str.equals("looking")) state = ZooKeeperPeerServer.ServerState.LOOKING;
        else if (str.equals("leading")) state = ZooKeeperPeerServer.ServerState.LEADING;
        else if (str.equals("following")) state = ZooKeeperPeerServer.ServerState.FOLLOWING;
        else if (str.equals("observing")) state = ZooKeeperPeerServer.ServerState.OBSERVING;
        else if (str.equals("observing_looking")) state = ZooKeeperPeerServer.ServerState.OBSERVING_LOOKING;
        return state;
    }

    private static void printError(Exception e, int port, String message)
    {
        synchronized (JavaRunner.class) {
            if (message.isEmpty())
                System.err.println("Error on port:" + port + " " + e.getMessage() + ", thread = " + Thread.currentThread());
            else
                System.err.println("Error on port:" + port + " " + e.getMessage() + ", thread = " + Thread.currentThread() + ", message = " + message);
        }
    }

    public static void printError(Exception e, int port, int level)
    {
        printError("", e, port, level);
    }

    public static void printError(String message, Exception e, int port, int level)
    {
        boolean print = false;
        for (int i : printingLevelsThatAreOn)
            if (level == i) {
                print = true;
                break;
            }
        //todo - turn this back on
        //if(print)
        printError(e, port, message);
    }

    public static byte[] longToBytes(long x)
    {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes)
    {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    private static ConcurrentHashMap<Long, PrintWriter> id_writer = new ConcurrentHashMap<>();
    public static volatile boolean printingToFileOn = false;

    public static void printToFile(String Gossip, long sid)
    {
        PrintWriter writer = id_writer.get(sid);
        try {
            if(writer == null){
                writer = new PrintWriter("Server"+sid+"gossip.txt", "UTF-8");
                id_writer.put(sid, writer);
            }
            writer.println(Gossip);
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void createGossipFile(long id)
    {
        printingToFileOn = true;
        try {
            File yourFile = new File("Server" + id + "gossip.txt");
            yourFile.createNewFile(); // if file already exists will do nothing
            FileOutputStream oFile = new FileOutputStream(yourFile, false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closePrinters()
    {
        for(long id : id_writer.keySet()) id_writer.get(id).close();
    }
}

















