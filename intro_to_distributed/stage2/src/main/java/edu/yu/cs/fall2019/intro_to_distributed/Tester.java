package edu.yu.cs.fall2019.intro_to_distributed;

public class Tester
{
    public static void main(String[] args)
    {
       testSerializing();
    }


    private static void testSerializing()
    {
        ElectionNotification en = new ElectionNotification(5, ZooKeeperPeerServer.ServerState.LOOKING, 1, 0);
        byte[] test = Util2.electionNotificationToByteArr(en);
        ElectionNotification en2 = Util2.byteArrToElectionNotification(test);

        boolean same = false;
        same = en.leader == en2.leader;
        same = en.sid == en2.sid;
        same = en.peerEpoch == en2.peerEpoch;
        same = en.state == en2.state;
        Util2.printReplace("same =  " + same);
    }
}
