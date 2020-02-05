package edu.yu.cs.fall2019.intro_to_distributed;

import java.io.*;

public class Util2
{
    /**
     * 0 : Always Off
     * 1 : Always On
     * 2 : Election Debugging
     * 201 : Election show votes
     * 3 : Election Complete
     * 4 : Server functionality
     */
    private static int[] printingLevelsThatAreOn = {1,2,3,4};
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
        System.out.println(s);
    }
    public static void printReplace(String s, int level)
    {
        boolean print = false;
        for(int i : printingLevelsThatAreOn) if(level == i){
            print = true;
            break;
        }
        if(print)System.out.println(s);
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

















