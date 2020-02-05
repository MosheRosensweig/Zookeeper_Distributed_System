package edu.yu.cs.fall2019.intro_to_distributed.stage4.faultTolerance;

import edu.yu.cs.fall2019.intro_to_distributed.ZooKeeperPeerServer;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.servers.ZooKeeperPeerServerImpl;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * There are 4 possible states that a server could be in when the occurs
 * [1] Looking
 * [2] Observing
 * [3] Following
 * [4] Leading
 * <p>
 * [1] Looking
 * - If a looking server elects a leader that has failed or generates a quorum based on a server
 * that failed - what should happen? I don't know... let it happen and...
 * [i]  if it elects a failed leader - try x number of times to connect to the leader, if they all fail,
 * then run a new election
 * [ii] if one of the quorum servers dies - who cares... I hope we don't end up with two leaders
 * <p>
 * [2] Observing
 * - If the server that failed is the LEADER, I care, other wise I do not.
 * - If a FOLLOWER failed - delete all references to it
 * - If a LEADER failed -
 * [i]   Kill all communication to it - i.e. the senders and receivers
 * [ii]  Delete all references to it
 * [iii] Run an election to connect to a new leader
 * <p>
 * [3] Following - exactly the same as observing. (The code to transition from a follower is elsewhere)
 * <p>
 * [4] Leading -
 * - The GATEWAY cannot die by definition, so don't worry about it
 * - For the FOLLOWER that failed
 * [i]   Kill all communication to it - i.e. the senders and receivers
 * [ii]  Find all work that was sent to it and send/assign that work to others
 * [iii] Delete all references to it
 */
public class DeadServerCleaner implements Runnable
{
    private ZooKeeperPeerServer parent;
    private Set<Integer> failedServersToBeDeleted;
    private Set<Integer> alreadyDeleted;
    private volatile boolean shutdown = false;

    public DeadServerCleaner(ZooKeeperPeerServer parent, Set<Integer> failedServersToBeDeleted, Set<Integer> alreadyDeleted)
    {
        this.parent = parent;
        this.failedServersToBeDeleted = failedServersToBeDeleted;
        this.alreadyDeleted = alreadyDeleted;
    }

    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public void run()
    {
        while (!shutdown) {
            if (failedServersToBeDeleted.isEmpty()) { // .remove() is only ever called, below
                Thread.yield();
                continue;
            }
            for (int port : failedServersToBeDeleted) {
                if (alreadyDeleted.contains(port) || port == parent.getMyPort()) break;
                switch (parent.getPeerState()) {
                    case LOOKING:
                    case OBSERVING_LOOKING:
                        //for now who cares
                        try {
                            Thread.sleep(1_000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //Thread.yield();//we don't do stuff during elections anyways
                        break;

                    case OBSERVING:
                    case FOLLOWING:
                    case LEADING:
                        deleteAServer(port);
                        failedServersToBeDeleted.remove(port);
                        alreadyDeleted.add(port);
                        break; // <-- I won't remove an then iterate
                }
                break;
            }

        }
    }

    private void deleteAServer(int port)
    {
        long leaderId = parent.getCurrentLeader().getCandidateID(); //you could be the leader
        InetSocketAddress leaderAdd = parent.getPeerByID(leaderId);
        int leaderPort = (leaderId == parent.getId()) ? parent.getMyPort() : leaderAdd.getPort();
        boolean isLeader = (port == leaderPort);
        long sid = ZooKeeperPeerServerImpl.getIdFromPort(parent.getPeerIDtoAddress(), port);
        if (isLeader) {
            deleteTheLeader(port, sid);
        } else {
            deleteAFollower(port, sid);
        }
    }

    private void deleteTheLeader(int port, long sid)
    {
        Util2.printReplace("Server:" + parent.getMyPort() + " is deleting the leader at port:" + port, 16);
        parent.genericKillTheLeader(port, sid);
    }

    private void deleteAFollower(int port, long sid)
    {
        Util2.printReplace("Server:" + parent.getMyPort() + " is deleting a FOLLOWER at port:" + port, 16);
        //the observer/follower is NOT connected to that server via tcp
        parent.killAFollower(port, sid);
    }


}
