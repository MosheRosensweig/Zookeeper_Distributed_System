package edu.yu.cs.fall2019.intro_to_distributed;

import edu.yu.cs.fall2019.intro_to_distributed.stage2.ZooKeeperPeerServerImpl;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ZooKeeperLeaderElection2
{
    private ZooKeeperPeerServerImpl peerServer;
    private HashSet<Long> followers;
    private Vote proposedLeader;
    private LinkedBlockingQueue<Message> incomingMessages;
    private boolean lookingForLeader = true;
    private int backoffMultiplyer = 1;

    public ZooKeeperLeaderElection2(ZooKeeperPeerServerImpl peerServer, LinkedBlockingQueue<Message> incomingMessages)
    {
        this.peerServer = peerServer;
        followers = new HashSet<>();
        proposedLeader = new Vote(peerServer.getId(), peerServer.getPeerEpoch(), peerServer.getPeerState());
        this.incomingMessages = incomingMessages;
    }

    private Vote elect()
    {
        while (lookingForLeader){
            broadcast();
            boolean hadMessages = checkMessages();
            if(hadMessages) {
                backoffMultiplyer = (backoffMultiplyer - 2 >= 1) ? backoffMultiplyer - 2 : 1;
                boolean foundWinner = checkForWinner();
                if (foundWinner) cleanUp();
            }
            else backoffMultiplyer++;

        }
        return proposedLeader;
    }

    /**
     * Read all available messages in the queue and handle them
     */
    private boolean checkMessages()
    {
        boolean hadMessages = false;
        long time = generateTime();
        while (lookingForLeader)
        {
            try
            {
                Message messageToSend = this.incomingMessages.poll(time, TimeUnit.SECONDS);
                if(messageToSend != null)
                {
                    hadMessages = true;
                    //todo parse and handle the message
                }
                if(!hadMessages) return false;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return hadMessages;
    }
    private long generateTime()
    {
        long time = 2;
        if(backoffMultiplyer > 1){
            Random r = new Random(backoffMultiplyer);
            int power = r.nextInt(backoffMultiplyer+1);
            time = (long) Math.pow(2,power);
        }
        return time;
    }
    /**
     * Determine if there is a winner.
     * If there is, than set proposedLeader to the winner.
     * @return true if there is a winner
     */
    private boolean checkForWinner()
    {
        boolean someoneWon = false;
        int quarum = peerServer.getQuorumSize();
        if(followers.size() >= quarum){
            //todo - check that other factors haven't happened
        }
        return someoneWon;
    }

    /**
     * Tell all the other servers that I'm looking for the leader
     * Give them my current vote for leader
     */
    private void broadcast()
    {
        ElectionNotification e = new ElectionNotification(proposedLeader.getCandidateID(),
                ZooKeeperPeerServer.ServerState.LOOKING,
                peerServer.getId(),
                proposedLeader.getPeerEpoch());
        byte[] contents = Util2.electionNotificationToByteArr(e);
        peerServer.sendBroadcast(Message.MessageType.ELECTION, contents);
    }
    private void cleanUp()
    {
        incomingMessages.clear();
        lookingForLeader = false;
    }

}
