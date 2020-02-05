package edu.yu.cs.fall2019.intro_to_distributed.stage2;
import edu.yu.cs.fall2019.intro_to_distributed.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ZooKeeperPeerServerImpl implements ZooKeeperPeerServer {

    private static long idGen = 0;
    private static int portGen = 8080;
    private static String host = "localhost";

    private final InetSocketAddress myAddress;
    private final long id;
    private final int port;
    private long peerEpoch;
    private HashMap<Long,InetSocketAddress> peerIDtoAddress;
    private LinkedBlockingQueue<Message> outgoingMessages;
    private LinkedBlockingQueue<Message> incomingMessages;
    private UDPMessageSender senderWorker;
    private UDPMessageReceiver receiverWorker;
    private volatile boolean shutdown;
    private ServerState state;
    private volatile Vote currentLeader;


    public ZooKeeperPeerServerImpl(int port, long peerEpoch, long id, HashMap<Long,InetSocketAddress>
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
        ZooKeeperLeaderElection election = new ZooKeeperLeaderElection(this,this.incomingMessages);
        return election.lookForLeader();
    }
    //<><><><><><><>//
    //  OVERRIDESS  //
    //<><><><><><><>//
    @Override
    public void shutdown()
    {

        this.shutdown = true;
        this.senderWorker.shutdown();
        this.receiverWorker.shutdown();
    }

    @Override
    public void run()
    {
        Util2.printReplace("Server " + id + " started", 4);
        //create the message sender and message receiver and turn them on
        senderWorker = new UDPMessageSender(outgoingMessages);
        receiverWorker = new UDPMessageReceiver(incomingMessages,myAddress, port);
        Util.startAsDaemon(senderWorker, "senderWorker for " + port);
        Util.startAsDaemon(receiverWorker, "receiverWorker for " + port);
        //
        try {
            while (!shutdown) {
                Util2.printReplace("Server " + id + " state = " + state, 0);
                switch (state) {
                    case LOOKING:
                        setCurrentLeader(lookForLeader());
                        break;
                    case LEADING:
                        //todo
                        break;
                    case FOLLOWING:
                        //todo
                        break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void setCurrentLeader(Vote v)
    {
        //the state should have already been set
        this.currentLeader = v;
        this.peerEpoch = v.getPeerEpoch();
        Util2.printReplace("Election concluded! Leader for server " + id + " is server " + currentLeader.getCandidateID()
                + ", my state is " + state + ". (The leading vote = " + currentLeader);
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
        for(long id : peerIDtoAddress.keySet()) sendMessage(type, messageContents, peerIDtoAddress.get(id));
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
        return peerEpoch;
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
        return (peerIDtoAddress.size()+1)/2 +1;
    }
}