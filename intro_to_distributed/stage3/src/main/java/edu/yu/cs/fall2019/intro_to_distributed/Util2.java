package edu.yu.cs.fall2019.intro_to_distributed;

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
     * 6 : TCP Sender
     * 604 : TCP Sender Error
     * 605 : TCP Sender Debug
     * 606 : TCP Sender deubg level 2
     * 7 : TCP Receiver
     * 704 : TCP Receiver Error
     * 705 : TCP Receiver Debug
     * 706 : TCP Receiver deubg level 2
     * 8 : Socket Handler
     * 804 : Socket Handler Error
     * 805 : Socket Handler Debug
     * 9
     * 905 : Gateway Feeder Debug
     * 10
     * 10_04 :
     * 11 : Simple Server
     * 11_05 : Simple Server Debug
     * 12 : ZooKeeperServer
     * 12_05 : ZooKeeperServer Debug
     * 13 : Gateway
     */
    private static int[] printingLevelsThatAreOn = {1};//{1,2,3,4,704,604,605,705,805,905,770, 11_05,706, 12_05};
    //https://stackoverflow.com/questions/2836646/java-serializable-object-to-byte-array
    public static byte[] electionNotificationToByteArr(ElectionNotification en)
    {
        byte[] contents = null;
        String asStr = en.leader + "," + en.state + "," + en.sid + "," + en.peerEpoch;
        contents = asStr.getBytes();
        return contents;
    }

    public static ElectionNotification byteArrToElectionNotification(byte[] b)
    {
        String[] feilds = new String(b).split(",");
        long leader = Long.parseLong(feilds[0]);
        ZooKeeperPeerServer.ServerState state = stringToState(feilds[1]);
        long sid = Long.parseLong(feilds[2]);
        long peerEpoch = Long.parseLong(feilds[3]);
        return new ElectionNotification(leader, state, sid, peerEpoch);//todo - implement
    }

    public static void printReplace(String s)
    {
        synchronized (JavaRunner.class){
            System.out.println(s);
        }
    }
    public static void printReplace(String s, int level)
    {
        boolean print = false;
        for(int i : printingLevelsThatAreOn) if(level == i){
            print = true;
            break;
        }
        if(print)printReplace(s);
    }

    public static Vote electionNotificationToVote(ElectionNotification en)
    {
        return new Vote(en.leader, en.peerEpoch, en.state);
    }

    public static ZooKeeperPeerServer.ServerState stringToState(String str)
    {
        ZooKeeperPeerServer.ServerState state = null;
        str = str.toLowerCase();
        if(str.equals("looking")) state = ZooKeeperPeerServer.ServerState.LOOKING;
        else if(str.equals("leading"))state = ZooKeeperPeerServer.ServerState.LEADING;
        else if(str.equals("following"))state = ZooKeeperPeerServer.ServerState.FOLLOWING;
        return state;
    }
}

















