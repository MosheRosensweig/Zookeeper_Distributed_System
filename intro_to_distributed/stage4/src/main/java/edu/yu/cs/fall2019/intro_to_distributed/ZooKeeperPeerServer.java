package edu.yu.cs.fall2019.intro_to_distributed;

import edu.yu.cs.fall2019.intro_to_distributed.election.Vote;

import java.net.InetSocketAddress;
import java.util.Map;

public interface ZooKeeperPeerServer extends Runnable
{
    void shutdown();

    @Override
    void run();

    void setCurrentLeader(Vote v);

    Vote getCurrentLeader();

    void sendMessage(Message.MessageType type, byte[] messageContents, InetSocketAddress target) throws IllegalArgumentException;

    void sendBroadcast(Message.MessageType type, byte[] messageContents);

    ServerState getPeerState();

    void setPeerState(ServerState newState);

    Long getId();

    long getPeerEpoch();

    InetSocketAddress getMyAddress();

    int getMyPort();

    InetSocketAddress getPeerByID(long id);

    int getQuorumSize();

    enum ServerState
    {
        LOOKING, FOLLOWING, LEADING, OBSERVING, OBSERVING_LOOKING;
        public char getChar()
        {
            switch(this){
                case LOOKING:
                    return 'O';
                case LEADING:
                    return 'E';
                case FOLLOWING:
                    return 'F';
                case OBSERVING:
                    return 'B';
                case OBSERVING_LOOKING:
                    return 'L';
            }
            return 'z';
        }
        public static ServerState getServerState(char c)
        {
            switch(c){
                case 'O':
                    return LOOKING;
                case 'E':
                    return LEADING;
                case 'F':
                    return FOLLOWING;
                case 'B':
                    return OBSERVING;
                case 'L':
                    return OBSERVING_LOOKING;
            }
            return null;
        }
    }

    ///////////////
    //  STAGE 4  //
    ///////////////
    public Map<Long, InetSocketAddress> getPeerIDtoAddress();
    public void genericKillTheLeader(int port, long sid);
    public void killAFollower(int port, long sid);
}
