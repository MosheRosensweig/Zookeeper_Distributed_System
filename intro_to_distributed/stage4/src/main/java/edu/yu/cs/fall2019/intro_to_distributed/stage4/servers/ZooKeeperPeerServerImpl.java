package edu.yu.cs.fall2019.intro_to_distributed.stage4.servers;

import edu.yu.cs.fall2019.intro_to_distributed.*;
import edu.yu.cs.fall2019.intro_to_distributed.election.ElectionNotification;
import edu.yu.cs.fall2019.intro_to_distributed.election.Vote;
import edu.yu.cs.fall2019.intro_to_distributed.election.ZooKeeperLeaderElection;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.faultTolerance.GossipHandler;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.follower.JavaRunnerFollower;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.leader.RoundRobinLeader;
import edu.yu.cs.fall2019.intro_to_distributed.udp.UDPMessageReceiver;
import edu.yu.cs.fall2019.intro_to_distributed.udp.UDPMessageSender;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ZooKeeperPeerServerImpl implements ZooKeeperPeerServer
{

    private static long idGen = 0;
    private static int portGen = 8080;
    private static String host = "localhost";

    private final InetSocketAddress myAddress;
    private final long id;
    private final int port;
    private volatile long peerEpoch;
    private AtomicLong atomicPeerEpoch;
    private Map<Long, InetSocketAddress> peerIDtoAddress; // should be concurrent
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private UDPMessageSender senderWorker;
    private UDPMessageReceiver receiverWorker;
    private volatile boolean shutdown;
    private volatile ServerState state;
    private volatile Vote currentLeader;

    private RoundRobinLeader rrl;
    private JavaRunnerFollower jrf;

    //////////
    //stage4//
    //////////
    private LinkedBlockingQueue<Message> incomingGossipMessages;
    private volatile boolean justFoundLeader = false;
    private Random r = new Random();
    //Gossip
    private GossipHandler gossipHandler;
    private Set<Integer> failedServersToBeDeleted = new ConcurrentHashMap<Integer, String>().newKeySet();
    //Gateway
    private boolean isGateway = false;
    private boolean existsActiveGateway = false; // all servers want to know so they can adjust the quorum size
    private GateWay gateWay;

    public ZooKeeperPeerServerImpl(int port, long peerEpoch, long id, ConcurrentHashMap<Long, InetSocketAddress>
            peerIDtoAddress)
    {
        this.id = id;
        this.port = port;
        this.peerEpoch = peerEpoch;
        this.peerIDtoAddress = peerIDtoAddress;
        this.myAddress = new InetSocketAddress(host, port);
        this.state = ServerState.LOOKING;

        outgoingMessages = new LinkedBlockingQueue<>();
        incomingMessages = new LinkedBlockingQueue<>();
        incomingGossipMessages = new LinkedBlockingQueue<>();

        this.atomicPeerEpoch = new AtomicLong(peerEpoch);
    }

    //<><><><><><><>//
    //  MY METHODS  //
    //<><><><><><><>//

    public static long generateNewServerId()
    {
        return idGen++;
    }

    public static int generateNewInetAddress()
    {
        int returnVal = portGen;
        portGen += 10; //this seems to be the convention Judah had
        return returnVal;
    }

    private Vote lookForLeader() throws InterruptedException
    {
        ZooKeeperLeaderElection election = new ZooKeeperLeaderElection(this, this.incomingMessages);
        return election.lookForLeader();
    }

    //<><><><><><><>//
    //   OVERRIDES  //
    //<><><><><><><>//
    @Override
    public void shutdown()
    {
        if (rrl != null) rrl.shutdown();
        if (jrf != null) jrf.shutdown();
        this.shutdown = true;
        if (senderWorker != null) this.senderWorker.shutdown();
        if (receiverWorker != null) this.receiverWorker.shutdown();
        if (gossipHandler != null) gossipHandler.shutdown();
        if (gateWay != null) gateWay.shutdown();
    }

    @Override
    public void run()
    {
        Util2.printReplace("Server " + id + " started", 4);
        //create the message sender and message receiver and turn them on
        senderWorker = new UDPMessageSender(outgoingMessages);
        receiverWorker = new UDPMessageReceiver(incomingMessages, myAddress, port, incomingGossipMessages);
        Util.startAsDaemon(senderWorker, "senderWorker for " + port);
        Util.startAsDaemon(receiverWorker, "receiverWorker for " + port);
        if (port == GateWay.GATEWAY_PORT) runAsGateWay();
        existsActiveGateway = systemHasGateWay();
        //
        try {
            while (!shutdown) {
                Util2.printReplace("Server " + id + " state = " + state, 0);
                switch (state) {
                    case LOOKING:
                    case OBSERVING_LOOKING:
                        setCurrentLeader(lookForLeader());
                        justFoundLeader = true;
                        //for lack of better place, I put this here
                        //I need to wait until the other servers are actually up...
                        if (gossipHandler == null) setupGossip();
                        break;
                    case LEADING:
                        if (justFoundLeader) runAsLeader();
                        justFoundLeader = false;
                        respondToElectionNotifications();
                        break;
                    case FOLLOWING:
                        if (justFoundLeader) runAsFollower();
                        justFoundLeader = false;
                        respondToElectionNotifications();
                        break;
                    case OBSERVING:
                        if (justFoundLeader) connectToLeader();
                        justFoundLeader = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void setCurrentLeader(Vote v)
    {
        //the state should have already been set
        this.currentLeader = v;
        setPeerEpoch(v.getPeerEpoch());
        Util2.printReplace("Election concluded! Leader for server " + id + " is server " + currentLeader.getCandidateID()
                + ", my state is " + state + ". (The leading vote = " + currentLeader, 3);
    }

    @Override
    public Vote getCurrentLeader()
    {
        return currentLeader;
    }

    @Override
    public void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException
    {
        outgoingMessages.add(new Message(type, messageContents, host, port, target.getHostName(), target.getPort()));
    }

    @Override
    public void sendBroadcast(Message.MessageType type, byte[] messageContents)
    {
        //peerIDtoAddress
        for (long id : peerIDtoAddress.keySet()) sendMessage(type, messageContents, peerIDtoAddress.get(id));
    }

    @Override
    public ServerState getPeerState() //get *my* state
    {
        return state;
    }

    @Override
    public void setPeerState(ServerState newState) //set *my* state
    {
        state = newState;
    }

    @Override
    public Long getId()
    {
        return id;
    }

    @Override
    public long getPeerEpoch()
    {
        //return peerEpoch;
        return atomicPeerEpoch.get();
    }

    private void setPeerEpoch(long newEpoch)
    {
        atomicPeerEpoch.set(newEpoch);
        peerEpoch = newEpoch;
    }

    @Override
    public InetSocketAddress getMyAddress()
    {
        return myAddress;
    }

    @Override
    public int getMyPort()
    {
        return port;
    }

    @Override
    public InetSocketAddress getPeerByID(long id)
    {
        return peerIDtoAddress.get(id);
    }

    @Override
    /**
     * @return the (number_of_servers/2) + 1
     */
    public int getQuorumSize()
    {
        // the size+1 is because we need to my own server is not in the list but counts
        if (!existsActiveGateway) return (peerIDtoAddress.size() + 1) / 2 + 1;
            // if there is a gateway, then the size = size +1 (for myself) -1 (for the gateway) = size + 0
        else return peerIDtoAddress.size() / 2 + 1;
    }

    //////////////
    //  STAGE3  //
    //////////////

    private void runAsLeader()
    {
        Util2.printReplace("Server<id:" + id + ",port:" + getMyPort() + "> is now LEADING", 12_05);
        LinkedBlockingQueue<Message> toSendToGateWay = null;
        if (jrf != null) {
            toSendToGateWay = transitionFromFollowerToLeader();
            rrl = new RoundRobinLeader(peerIDtoAddress, getMyPort(), toSendToGateWay);
        } else rrl = new RoundRobinLeader(peerIDtoAddress, getMyPort());
        Util.startAsDaemon(rrl, "Round Robin Leader");
    }

    private void runAsFollower()
    {
        Util2.printReplace("Server<id:" + id + ",port:" + getMyPort() + "> is now FOLLOWING server id:" + currentLeader.getCandidateID(), 12_05);
        InetSocketAddress leader = peerIDtoAddress.get(currentLeader.getCandidateID());
        int leaderPort = leader.getPort();
        String leaderHost = leader.getHostName();
        Util2.printReplace("leaderPort:" + leaderPort + ", leaderHost:" + leaderHost + ", myPort:" + getMyPort(), 12_05);
        jrf = new JavaRunnerFollower(leaderPort, leaderHost, getMyPort());
        new Thread(jrf).start();
    }

    //////////////
    //  STAGE4  //
    //////////////

    /**
     * Do this one at a time
     */
    private void respondToElectionNotifications()
    {
        if (incomingMessages.isEmpty()) {
            Thread.yield();
            //Util2.printReplace("Server:"+port+", in state:"+state+", doesn't have any messages", 4_1);
            return;
        } else {
            Message message = incomingMessages.poll();
            ElectionNotification en = Util2.byteArrToElectionNotification(message.getMessageContents());
            if (en.state == ZooKeeperPeerServer.ServerState.LOOKING) {
                ElectionNotification send = new ElectionNotification(currentLeader.getCandidateID(),
                        state,
                        id,
                        currentLeader.getPeerEpoch());
                sendMessage(Message.MessageType.ELECTION, Util2.electionNotificationToByteArr(send), peerIDtoAddress.get(en.sid));
                //sendBroadcast(Message.MessageType.ELECTION, Util2.electionNotificationToByteArr(send));
                Util2.printReplace("Server:" + port + ", in state:" + state + ", responding to server id:" + en.sid + " at port#" + peerIDtoAddress.get(en.sid).getPort(), 4_1);
            }
        }
    }

    private void setupGossip()
    {
        gossipHandler = new GossipHandler(getMyPort(), this, incomingGossipMessages, failedServersToBeDeleted);
        Util.startAsDaemon(gossipHandler, "gossipHandler for gateway:" + getMyPort());
    }

    /**
     * @return a random server that is not me - because peerIDtoAddress doesn't include myself
     */
    public static InetSocketAddress getRandomServer(Map<Long, InetSocketAddress> map, Random r)
    {
        int random = r.nextInt(map.size());
        int i = 0;
        InetSocketAddress result = null;
        for (Long id : map.keySet()) {
            if (i++ == random) result = map.get(id);
        }
        return result;
    }

    public Map<Long, InetSocketAddress> getPeerIDtoAddress()
    {
        return peerIDtoAddress;
    }

    public static long getIdFromPort(Map<Long, InetSocketAddress> inputPeerIDtoAddress, long port)
    {
        long leaderId = -1;
        for (long id : inputPeerIDtoAddress.keySet()) {
            if (inputPeerIDtoAddress.get(id).getPort() == port) {
                leaderId = id;
                break;
            }
        }
        if (leaderId == -1)
            throw new IllegalStateException(" Attempted to access a server that doesn't exist, attempted port = " + port);
        return leaderId;
    }

    //////////////////////
    //  FAULT TOLERANCE //
    //////////////////////

    /**
     * DeadServerCleaner calls this
     *
     * @param port - of the server to kill
     * @param sid  - of the server to kill
     */
    public void genericKillTheLeader(int port, long sid)
    {
        if (sid < 0)
            throw new IllegalStateException("Follower <sid:" + id + ",port:" + port + "> cannot delete a server that doesn't exist at sid:" + sid);
        if (state == ServerState.LEADING)
            throw new IllegalStateException("Leader, sid:" + id + ", port:" + port + ", cannot delete itself!");
            //that could happen but I haven't decided what to do, so for now, throw an exception
        else if (state == ServerState.FOLLOWING) {
            followerKillLeader(port, sid);
        } else { //Observing
            gatewayKillLeader(port, sid);
        }
        peerIDtoAddress.remove(sid);
        long oldEpoch = atomicPeerEpoch.get();
        atomicPeerEpoch.set(oldEpoch + 1);
        Util2.printReplace(state + " <sid:" + id + ",port:" + port + "> oldEpoch:" + oldEpoch + ", newEpoch:" + atomicPeerEpoch.get() + " is about to start a new election", 16);
        //do this at the end, because it starts the next election
        if (isGateway) state = ServerState.OBSERVING_LOOKING;
        else state = ServerState.LOOKING;//run a new election
    }

    /**
     * DeadServerCleaner calls this
     *
     * @param port - of the server to kill
     * @param sid  - of the server to kill
     */
    public void killAFollower(int port, long sid)
    {
        //if it's the leader, do extra stuff
        if (state == ServerState.LEADING) leaderKillFollower(port, sid);
        //all servers do this cleanup
        peerIDtoAddress.remove(sid);
    }

    private void leaderKillFollower(int port, long sid)
    {
        if(rrl == null) {
            Util2.printReplace("server: " + id + ", port:" + this.port + ", rrl = " + rrl + ", trying to kill server:" + sid + ", port:" + port, 16);
            try {
                Thread.sleep(2_500); // <-- give the OS time to spin up that thread, this is a legit race condition - crazy
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        rrl.killFollower(port, sid);
    }

    private void followerKillLeader(int port, long sid)
    {
        if (jrf == null)
            throw new IllegalStateException("Follower <sid:" + id + ",port:" + port + "> is trying to kill a leader, but isn't yet set up as a JRF");
        jrf.deleteOldLeader();//kill connections with the leader (i.e. the sender/receiver)
    }

    private void gatewayKillLeader(int port, long sid)
    {
        gateWay.shutdownTCPConnectionToLeader();
    }

    private LinkedBlockingQueue<Message> transitionFromFollowerToLeader()
    {
        //Take all completed work from the JRF and add it to the queue to send to the GATEWAY
        //Take any incomplete work and add it to the "from gateway queue"
        //then shutdown the jrf
        while (jrf.stillHaveWorkToDo()) Thread.yield();
        try {
            Thread.sleep(1_000); // give the follower threads time to finish
        } catch (InterruptedException e) {
            Util2.printError(e, port, 16);
        }
        LinkedBlockingQueue<Message> toSendToGateWay = jrf.getOutgoingMessages();
        jrf.shutdown();
        jrf = null;
        return toSendToGateWay;
    }

    ///////////////
    //  GATEWAY  //
    ///////////////
    private void runAsGateWay()
    {
        //todo implement
        isGateway = true;
        state = ServerState.OBSERVING_LOOKING;
        gateWay = new GateWay(this);
        Util.startAsDaemon(gateWay, "Gateway");
    }

    public boolean isGateway()
    {
        return isGateway;
    }

    private boolean systemHasGateWay()
    {
        if (isGateway) return true;
        else {
            for (long sid : peerIDtoAddress.keySet()) {
                InetSocketAddress add = peerIDtoAddress.get(sid);
                if (add.getPort() == GateWay.GATEWAY_PORT) return true;
            }
        }
        return false;
    }

    private void connectToLeader()
    {
        InetSocketAddress leaderAdd = peerIDtoAddress.get(currentLeader.getCandidateID());
        gateWay.connectToNewLeader(leaderAdd.getPort(), leaderAdd.getHostName());
    }
}