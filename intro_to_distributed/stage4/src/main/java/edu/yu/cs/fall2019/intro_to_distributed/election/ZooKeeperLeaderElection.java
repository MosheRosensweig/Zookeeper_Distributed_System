package edu.yu.cs.fall2019.intro_to_distributed.election;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer.ServerState.*;

public class ZooKeeperLeaderElection
{
    /**
     * time to wait once we believe we've reached the end of leader election.
     */
    private final static int finalizeWait = 200;
    /**
     * Upper bound on the amount of time between two consecutive notification checks.
     * This impacts the amount of time to get the system up again after long partitions. Currently 60 seconds.
     */
    private final static int maxNotificationInterval = 60000;
    private final static int defaultWait = 2;

    private LinkedBlockingQueue<Message> incomingMessages;
    private ZooKeeperPeerServer myPeerServer;
    private long proposedLeader;
    private long proposedEpoch;
    private int backoffMultiplyer = 1;
    private Map<Long, Vote> voteMap;

    private boolean isGateway = false;

    public ZooKeeperLeaderElection(ZooKeeperPeerServer server, LinkedBlockingQueue<Message> incomingMessages)
    {
        this.incomingMessages = incomingMessages;
        this.myPeerServer = server;
        proposedLeader = myPeerServer.getId();
        isGateway = myPeerServer.getMyPort()  == GateWay.GATEWAY_PORT;
        if(isGateway) proposedLeader = -1;
        proposedEpoch = myPeerServer.getPeerEpoch();
        voteMap = new ConcurrentHashMap<>();
    }

    public synchronized Vote getVote()
    {
        return new Vote(this.proposedLeader, this.proposedEpoch, myPeerServer.getPeerState());
    }

    public synchronized Vote lookForLeader() throws InterruptedException
    {
        Vote result = null;
        ElectionNotification en = null;
        //add myself to the map
        voteMap.put(myPeerServer.getId(), getVote());
        //send initial notifications to other peers to get things started
        sendNotifications();
        while (this.myPeerServer.getPeerState() == LOOKING || this.myPeerServer.getPeerState() == OBSERVING_LOOKING) {
            //get the next message and parse it
            if (en == null) en = getNextElecNot();
            Vote newVote = Util2.electionNotificationToVote(en);
            Util2.printReplace("Server " + myPeerServer.getId() + " got a vote for " + newVote.getCandidateID(), 201);
            //record the vote
            if(newVote.getPeerEpoch() >= proposedEpoch && (newVote.getState() != OBSERVING_LOOKING)) {
                voteMap.put(en.sid, newVote);
            }
            //parse the state
            switch (en.state) {
                case LOOKING:
                    if (newVoteSupersedesCurrent(newVote.getCandidateID(), newVote.getPeerEpoch(), proposedLeader, proposedEpoch))
                        updateMyVote(newVote);
                    if (haveEnoughVotes(voteMap, getVote())) {                      //check if the *proposed leader* now has enough votes
                        Util2.printReplace("Server " + myPeerServer.getId() + " has enough votes for  " + newVote.getCandidateID() + " (I got a LOOKING)", 2);
                        wait(finalizeWait);
                        ElectionNotification betterEN = getBetterEN();       //double check that there isn't another better vote
                        if (betterEN == null) result = acceptElectionWinner(en);    //if there is no better vote, accept
                        else en = betterEN;
                    } else en = null;
                    break;
                case LEADING:
                case FOLLOWING:
                    //if (haveEnoughVotes(voteMap, getVote() )) {
                    if (haveEnoughVotes(voteMap, newVote)) {
                        Util2.printReplace("Server " + myPeerServer.getId() + " has enough votes for  " + newVote.getCandidateID() + " (I got a LEADING/LOOKING) = " + newVote.getState(), 2);
                        result = acceptElectionWinner(en);
                    }else en = null;
                    break;
                case OBSERVING_LOOKING:
                    sendNotifications();
                    en = null;
                    break;
            }
        }
        return result;
    }

    /**
     * //@return true if there is a vote that supersedes my current winning vote
     *
     * @return null if there is no better en, otherwise return the better en
     */
    private ElectionNotification getBetterEN() // I don't think I need this param
    {
        try {
            Message messageToSend = this.incomingMessages.poll(defaultWait, TimeUnit.SECONDS);//get the first vote
            while (messageToSend != null) {
                ElectionNotification en = Util2.byteArrToElectionNotification(messageToSend.getMessageContents());
                Vote v = Util2.electionNotificationToVote(en);
                Util2.printReplace("Server " + myPeerServer.getId() + " got a vote for " + v.getCandidateID(), 201);
                if(v.getState() != OBSERVING_LOOKING) {//only add regular votes
                    voteMap.put(en.sid, v); // we need to do this regardless
                    boolean betterVote = newVoteSupersedesCurrent(v.getCandidateID(), v.getPeerEpoch(), proposedLeader, proposedEpoch);
                    //if I got a better vote - go back to the regular cycle,
                    //if the vote is for the current leader, well then it doesn't matter, becuase the leader has a quorum already
                    if (betterVote) {
                        Util2.printReplace("Server " + myPeerServer.getId() + " got a better vote and is now voting for " + v.getCandidateID(), 2);
                        return en;
                    }
                }
                messageToSend = this.incomingMessages.poll(defaultWait, TimeUnit.SECONDS); //get the next message
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private Vote acceptElectionWinner(ElectionNotification n)
    {
        Util2.printReplace("Server " + myPeerServer.getId() + " Finished the election ", 2);
        //set my state to either LEADING or FOLLOWING
        ZooKeeperPeerServer.ServerState old = myPeerServer.getPeerState();
        if(old == OBSERVING_LOOKING) myPeerServer.setPeerState(OBSERVING);
        else myPeerServer.setPeerState((n.leader == myPeerServer.getId()) ? LEADING : FOLLOWING);
        //clear out the incoming queue before returning
        incomingMessages.clear();
        return Util2.electionNotificationToVote(n);
    }

    /*
     * We return true if one of the following three cases hold:
     * 1- New epoch is higher
     * 2- New epoch is the same as current epoch, but server id
     */
    protected boolean newVoteSupersedesCurrent(long newId, long newEpoch, long curId, long curEpoch)
    {
        return (newEpoch > curEpoch) || ((newEpoch == curEpoch) && (newId > curId));
    }

    /**
     * Termination predicate. Given a set of votes, determines if have sufficient to declare the end of the election round.
     * I don't count who voted for who, since as I receive them I will automatically change my vote to the highest sid, as will
     * everyone else
     */
    protected boolean haveEnoughVotes(Map<Long, Vote> votes, Vote vote)
    {
        int inFavor = 0;
        for (Map.Entry<Long, Vote> entry : votes.entrySet()) {
            if (vote.equals(entry.getValue())) {
                inFavor++;
            }
        }
        return this.myPeerServer.getQuorumSize() <= inFavor;
    }

    //////////////
    //  My Code //
    //////////////

    /**
     * Tell all the other servers that I'm looking for the leader
     * Give them my current vote for leader
     */
    private void sendNotifications()
    {
        Util2.printReplace("Server " + myPeerServer.getId() + ", port: " + myPeerServer.getMyPort() + " sent notifications. Vote for " + proposedLeader, 2);
        ElectionNotification e = new ElectionNotification(proposedLeader,
                myPeerServer.getPeerState(),
                myPeerServer.getId(),
                proposedEpoch);
        byte[] contents = Util2.electionNotificationToByteArr(e);
        myPeerServer.sendBroadcast(Message.MessageType.ELECTION, contents);
    }

    /**
     * Get the next message and convert it into an ElectionNotification.
     * This method will check for messages using an exponential backoff
     * but the max wait time will max out at maxNotificationInterval.
     *
     * @return the ElectionNotification included in a message
     */
    private ElectionNotification getNextElecNot()
    {
        ElectionNotification en = null;
        backoffMultiplyer = 1;
        long time = generateTime();
        try {
            while (en == null) {
                long test1 = System.currentTimeMillis();
                Message messageToSend = this.incomingMessages.poll(time, TimeUnit.SECONDS);
                long now = System.currentTimeMillis();
                long total_time = (now - test1) / 1000;
                if (messageToSend != null) {
                    en = Util2.byteArrToElectionNotification(messageToSend.getMessageContents());
                } else {
                    sendNotifications();
                    backoffMultiplyer += 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return en;
    }

    private long generateTime()
    {
        long time = 2;
        if (backoffMultiplyer > 1) {
            Random r = new Random(backoffMultiplyer);
            int power = r.nextInt(backoffMultiplyer + 1);
            time = (long) Math.pow(defaultWait, power);
        }
        time = (time < maxNotificationInterval) ? time : maxNotificationInterval;
        return time;
    }

    private void updateMyVote(Vote newVote)
    {
        proposedLeader = newVote.getCandidateID();
        proposedEpoch = newVote.getPeerEpoch();
        voteMap.put(myPeerServer.getId(), getVote());
        sendNotifications();// tell everyone about my updated vote
    }

    private boolean isValidServer(ElectionNotification en)
    {
        long fromID = en.sid;
        long voteID = en.leader;
        return true;
    }
}
