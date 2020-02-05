package edu.yu.cs.fall2019.intro_to_distributed.stage4.faultTolerance;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HeartBeatGossipSender implements Runnable
{
    private volatile boolean shutdown = false;
    private GossipHandler gossipHandler;
    private Random r = new Random();

    public HeartBeatGossipSender(GossipHandler gossipHandler)
    {
        this.gossipHandler = gossipHandler;
    }

    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public void run()
    {
        while (!shutdown) {
            sendHeartBeats();
            sendGossip();
            checkForDeadServers();
            gossipHandler.incrementHeartBeatCount();
            try {
                Thread.sleep(GossipHandler.roundTripTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void checkForDeadServers()
    {
        //[1] Check all servers to make sure they are still "alive"
        Map<Integer, GossipHandler.Gossip> gossipMap = gossipHandler.getGossipMap();
        for(int port : gossipMap.keySet()){//for all servers
            GossipHandler.Gossip gossip = gossipMap.get(port);
            long time = System.currentTimeMillis();
            double timeDif = (time - gossip.time)/GossipHandler.roundTripTime; //check how many rtt's it's been since we last heard from them
            //[2] Check if a server failed
            if(timeDif >=3 && timeDif < 6){ //if it's between fail and cleanup
               if(!gossipHandler.alreadyDeleted.contains(port)){
                   gossipHandler.failedServersToBeDeleted.add(port); //mark it for deletion
               }
            }
            //[3] Check if a server needs to be cleaned up yet
            else if(timeDif >= 6){
                gossip.isDead = true;
                if(gossipHandler.alreadyDeleted.contains(port)){
                    gossipMap.remove(port);
                }
            }
        }
        //[4] delete any servers that are dead and we have already sent out that info once
//        for(int port : gossipHandler.deadServers){
//            GossipHandler.Gossip gossip = gossipMap.get(port);
//            if(gossip != null && gossip.sendDeadCounter == 1) gossipMap.remove(port);
//        }
    }

    private void sendHeartBeats()
    {
        byte[] contents = generateHeartBeatContents(gossipHandler.myPort, gossipHandler.myHeartBeatCount);
        gossipHandler.parent.sendBroadcast(Message.MessageType.HEARTBEAT, contents);
    }

    /**
     * @return a byte[] with the first bytes representing the port (long) and then the counter (long)
     */
    public static byte[] generateHeartBeatContents(long myport, long count)
    {
        //just <port, counter>
        byte[] port = Util2.longToBytes(myport);
        byte[] counter = Util2.longToBytes(count);
        byte[] combo = new byte[16]; // a long is 8 bytes
        int i = 0;
        for (; i < 8; i++) combo[i] = port[i];
        for (; i < 16; i++) combo[i] = counter[i-8];
        return combo;
    }//tested that this works

    public static long[] heartBeatToPortCounter(byte[] contents)
    {
        byte[] port = new byte[8];
        byte[] counter = new byte[8];
        for (int i = 0; i < 8; i++) port[i] = contents[i];
        for (int i = 8; i < 16; i++) counter[i-8] = contents[i];
        long[] res = new long[2];
        res[0] = Util2.bytesToLong(port);
        res[1] = Util2.bytesToLong(counter);
        return res;
    }//tested that this works

    private void sendGossip()
    {
        byte[] contents = gossipMapToBtyeArr(gossipHandler.getGossipMap());
        Map<Long, InetSocketAddress> peerMap = gossipHandler.parent.getPeerIDtoAddress();
        InetSocketAddress toSend = ZooKeeperPeerServerImpl.getRandomServer(peerMap, r);
        gossipHandler.parent.sendMessage(Message.MessageType.GOSSIP, contents, toSend);
    }

    /**
     * NOTE: This is not for generic use!
     * It has logic in it to change data if the server is isDead <-- took it out
     * @param gossipMap
     * @return
     */
    private static byte[] gossipMapToBtyeArr(Map<Integer, GossipHandler.Gossip> gossipMap)
    {
        byte[] result;
        StringBuilder sb = new StringBuilder();
        //synchronized (gossipMap) { //<-- its a ConcurrentHashMap now
            for (int port : gossipMap.keySet()) {
                GossipHandler.Gossip gossip = gossipMap.get(port);
                long counter = gossip.counter;
                sb.append(port + "," + counter + ";");
            }
        //}
        result = sb.toString().getBytes();
        return result;
    }//tested that this works

    /**
     * Note: This is not a direct reverse of serialized map
     * @param contents
     * @return
     */
    public static Map<Integer, Long> byteArrToGossipMap(byte[] contents)
    {
        Map<Integer, Long> res = new HashMap<>();
        String start = new String(contents);
        String[] pairs = start.split(";");
        if(start.isEmpty()) return null; // the gossip map hasn't been built yet
        for (String pair : pairs) {
            String[] keyVal = pair.split(",");
            int port = (int) Long.parseLong(keyVal[0]);
            long counter = Long.parseLong(keyVal[1]);
            res.put(port, counter);
        }
        return res;
    }//tested that this works

}
