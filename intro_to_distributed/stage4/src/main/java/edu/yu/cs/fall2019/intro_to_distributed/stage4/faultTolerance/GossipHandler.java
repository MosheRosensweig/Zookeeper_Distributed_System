package edu.yu.cs.fall2019.intro_to_distributed.stage4.faultTolerance;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The GossipHandler - sets everything up and then runs the processing
 * HeartBeatSender - a thread that every roundTripTime seconds sends out n heartbeats
 * and 1 gossip (make sure not to itself)
 * The UDPReceiver receives heartbeats and gossips, and puts them in the incomingGossipMessages queue
 */
public class GossipHandler implements Runnable
{
    protected int myPort;
    protected volatile long myHeartBeatCount = 0;
    private Map<Integer, Gossip> gossipMap = new ConcurrentHashMap<>(); // port, gossip // DO INCLUDE YOURSELF
    protected ZooKeeperPeerServer parent;
    private LinkedBlockingQueue<Message> incomingGossipMessages;
    // concurrent hashset
    // servers that I have found to be failed, but haven't told everyone else.
    // these severs have been quite for >= failureTime, but < cleanupTime
    // once in here, no going back
    // the cleaner will remove anything in here from operating
    protected Set<Integer> failedServersToBeDeleted; //new ConcurrentHashMap<Integer, String>().newKeySet()
    protected Set<Integer> alreadyDeleted; //new ConcurrentHashMap<Integer, String>().newKeySet()
    private volatile boolean shutdown = false;

    //stuff to pass
    public static final long roundTripTime = 2_000; // (ms). "Send heartbeat every T time."
    public static final long failureTimeMultiplyer = 3;
    public static final long cleanuptimeMultiplyer = 2;
    public static final long failureTime = failureTimeMultiplyer * roundTripTime;
    public static final long cleanuptime = cleanuptimeMultiplyer * failureTime;

    private HeartBeatGossipSender heartBeatGossipSender;
    private DeadServerCleaner deadServerCleaner;

    public GossipHandler(int myPort, ZooKeeperPeerServer parent, LinkedBlockingQueue<Message> incomingGossipMessages, Set<Integer> failedServersToBeDeleted)
    {
        this.myPort = myPort;
        this.parent = parent;
        this.incomingGossipMessages = incomingGossipMessages;
        this.failedServersToBeDeleted = failedServersToBeDeleted;
        this.alreadyDeleted = new ConcurrentHashMap<>().newKeySet();
    }

    public void shutdown()
    {
        if (heartBeatGossipSender != null) heartBeatGossipSender.shutdown();
        if(deadServerCleaner != null) deadServerCleaner.shutdown();
        shutdown = true;
    }


    @Override
    public void run()
    {
        setUpSender();
        setupDeadServerCleaner();
        while (!shutdown) {
            try {
                Message m = incomingGossipMessages.poll(2, TimeUnit.SECONDS); // I need to be able to catch a shutdown so I am using poll instead of take
                if (m == null) {
                    Thread.yield();
                    continue;
                }
                if (m.getMessageType() == Message.MessageType.HEARTBEAT) processHeartBeat(m);
                else if (m.getMessageType() == Message.MessageType.GOSSIP) processGossip(m);
                else throw new IllegalStateException("GossipHandler: "+myPort+" got a non-HeartBeat/non-Gossip message, type:"+m.getMessageType());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setUpSender()
    {
        heartBeatGossipSender = new HeartBeatGossipSender(this);
        Util.startAsDaemon(heartBeatGossipSender, "heartBeatGossipSender for port:" + myPort);
    }

    /**
     * If we got messages from this server already, update it
     * otherwise add that server
     *
     * @param m
     */
    private void processHeartBeat(Message m)
    {
        long[] port_counter = HeartBeatGossipSender.heartBeatToPortCounter(m.getMessageContents());
        int port = (int) port_counter[0];
        processHeartBeat(port, port_counter[1]);
    }

    /**
     * New understanding of Gossip = it never gets explicit information about deadness only
     * gives updates about liveliness
     * @param port
     * @param counter
     */
    private void processHeartBeat(int port, long counter)
    {
        if(port == myPort || alreadyDeleted.contains(port)) return; // <-- don't add yourself - it makes things simpler
        long currentTime = System.currentTimeMillis();
        Gossip gossip = gossipMap.get(port);
        if (gossip == null) {
            gossip = new Gossip(counter, currentTime);
        } else {
            //here, only make updates about liveliness, not deadness
            boolean isUpdate = gossip.counter < counter //there is higher counter
                    && !failedServersToBeDeleted.contains(port); //it has been marked as failed
            if(isUpdate) {
                gossip.time = currentTime;
                gossip.counter = Math.max(gossip.counter, counter);
            }
        }
        // [1] Add a new Live server
        // [2] Re-add a server that I already knew about with no changes
        // [3] Re-add a server that I already knew about with a new Gossip
        gossipMap.put(port, gossip);
    }

    private void setupDeadServerCleaner()
    {
        deadServerCleaner = new DeadServerCleaner(parent,failedServersToBeDeleted, alreadyDeleted);
        Util.startAsDaemon(deadServerCleaner, "deadServerCleaner on port:" + parent.getMyPort());
    }

    private void processGossip(Message m)
    {
        Map<Integer, Long> port_counter = HeartBeatGossipSender.byteArrToGossipMap(m.getMessageContents());
        if(port_counter == null) return;
//        Util2.printReplace("Server:"+myPort +" got a non-null gossip message, map = " + port_counter +
//                ", from server:" + m.getSenderPort() + ", my current total is = " + gossipMap.size()
//                + "\t--> actual map before processing - " + gossipMapToString(), 15);
//                //+"\n\t--> actual map before processing - " + gossipMapToString(), 15);

        Util2.printReplace("Server:"+myPort +" got a non-null gossip message, from server:" + m.getSenderPort() + ", my current total is = " + gossipMap.size() +
                ", " + gossipMapToString(), 15);
        if(Util2.printingToFileOn){
            String toPrint = "Received a gossip Message from server on port:" + m.getSenderPort() +
                    "\n\tCurrently there are this many servers alive = " + gossipMap.size() +
                    "\n\tCurrently my gossip map is: " + gossipMapToString() +
                    "\n\tThe received gossip map is: " + receivedGossipMapToString(port_counter);
            Util2.printToFile(toPrint, parent.getId());
        }
        for (int port : port_counter.keySet()) {
            long counter = port_counter.get(port);
            processHeartBeat(port, counter);
        }
    }

    public void incrementHeartBeatCount()
    {
        myHeartBeatCount++;
    }

    protected Map<Integer, Gossip> getGossipMap()
    {
        return gossipMap;
    }

    protected static class Gossip
    {
        long counter;
        long time;
        boolean isDead = false; //true if it's been silent for cleanupTime
        int sendDeadCounter = 0;

        public Gossip(long counter, long time)
        {
            this.counter = counter;
            this.time = time;
        }
    }

    private String gossipMapToString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("time = "+System.currentTimeMillis()+" <Port,Counter> -->{");
        for(int i : gossipMap.keySet()){
            Gossip g = gossipMap.get(i);
            sb.append(i+":"+g.counter+", ");
        }
        sb.append("}");
        return sb.toString();
    }
    private String receivedGossipMapToString(Map<Integer, Long> port_counter)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("time = "+System.currentTimeMillis()+" <Port,Counter> -->{");
        for(int i : port_counter.keySet()){
            sb.append(i+":"+port_counter.get(i)+", ");
        }
        sb.append("}");
        return sb.toString();
    }
}
