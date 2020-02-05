package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class Tester
{
    private static final int gateway_port = 7090;
    private static final int leader_port = 8080;
    private static final int worker_port = 8090;

    private static Message message = new Message(Message.MessageType.COMPLETED_WORK,
            "Hello".getBytes(),
            "localhost",
            7090,
            "localhost",
            8080,
            1);

    public static void main(String[] args) throws InterruptedException
    {
        //test1();
        //test2Followers();
        //testNFollowers(3);
        //testNFollowersGateWay(2);
        testGatewayInsideLeaderNFollowers(2);
    }

    private static void test1() throws InterruptedException
    {
        LinkedBlockingQueue<Message> fromGateway = new LinkedBlockingQueue<>();
        int i = 1;
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        RoundRobinLeader rrl = new RoundRobinLeader(null, 8080, fromGateway);
        JavaRunnerFollower jrf = new JavaRunnerFollower(8080, "localhost", 8090);
        new Thread(rrl).start();
        new Thread(jrf).start();
        Thread.sleep(1_000);
        System.out.println("shutting down");
        rrl.shutdown();
        jrf.shutdown();

    }

    private static void testGatewayInsideLeaderNFollowers(int n) throws InterruptedException
    {

        RoundRobinLeader rrl = new RoundRobinLeader(null, 8080);
        ArrayList<JavaRunnerFollower> jrfs = new ArrayList<>();
        int staringPort = 9000;
        for(int a = 0; a < n; a++){
            jrfs.add(new JavaRunnerFollower(8080, "localhost", staringPort));
            staringPort += 5;
        }
        new Thread(rrl).start();
        Thread.sleep(10);
        for(JavaRunnerFollower jr : jrfs) new Thread(jr).start();
        Thread.sleep(2_000);
        System.out.println("finishing test....");
    }

    private static void testNFollowersGateWay(int n) throws InterruptedException
    {
        LinkedBlockingQueue<Message> fromGateway = new LinkedBlockingQueue<>();
        int i = 1;
        for (i = 1; i < 50; i++) fromGateway.put(generateMessage("Sending Message #" + i));

//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
//        fromGateway.put(generateMessage("Sending Message #" + i++));
        RoundRobinLeader rrl = new RoundRobinLeader(null, 8080, fromGateway);
        ArrayList<JavaRunnerFollower> jrfs = new ArrayList<>();
        GateWay gateWay = new GateWay();
        gateWay.setLeaderPort(leader_port);
        gateWay.setLeaderHost("localhost");
        int staringPort = 9000;
        for(int a = 0; a < n; a++){
            jrfs.add(new JavaRunnerFollower(8080, "localhost", staringPort));
            staringPort += 5;
        }
        new Thread(rrl).start();
        Thread.sleep(10);
        new Thread(gateWay).start();
        for(JavaRunnerFollower jr : jrfs) new Thread(jr).start();
        Thread.sleep(2_000);
//        System.out.println("shutting down");
//        rrl.shutdown();
//        gateWay.shutdown();
//        for(JavaRunnerFollower jr : jrfs) jr.shutdown();
        System.out.println("finishing test....");
    }

    private static void testNFollowers(int n) throws InterruptedException
    {
        LinkedBlockingQueue<Message> fromGateway = new LinkedBlockingQueue<>();
        int i = 1;
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        RoundRobinLeader rrl = new RoundRobinLeader(null, 8080, fromGateway);
        ArrayList<JavaRunnerFollower> jrfs = new ArrayList<>();
        int staringPort = 9000;
        for(int a = 0; a < n; a++){
            jrfs.add(new JavaRunnerFollower(8080, "localhost", staringPort));
            staringPort += 5;
        }
        new Thread(rrl).start();
        for(JavaRunnerFollower jr : jrfs) new Thread(jr).start();
        Thread.sleep(2_000);
        System.out.println("shutting down");
        rrl.shutdown();
        for(JavaRunnerFollower jr : jrfs) jr.shutdown();
        System.out.println("finishing test....");
    }

    private static void test2Followers() throws InterruptedException
    {
        LinkedBlockingQueue<Message> fromGateway = new LinkedBlockingQueue<>();
        int i = 1;
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        fromGateway.put(generateMessage("Sending Message #" + i++));
        RoundRobinLeader rrl = new RoundRobinLeader(null, 8080, fromGateway);
        JavaRunnerFollower jrf = new JavaRunnerFollower(8080, "localhost", 8090);
        JavaRunnerFollower jrf2 = new JavaRunnerFollower(8080, "localhost", 8095);
        new Thread(rrl).start();
        new Thread(jrf).start();
        new Thread(jrf2).start();
        Thread.sleep(1_500);
        System.out.println("shutting down");
        rrl.shutdown();
        jrf.shutdown();
        jrf2.shutdown();

    }

    private static Message generateMessage(String str)
    {
        return new Message(Message.MessageType.COMPLETED_WORK,
                str.getBytes(),
                "localhost",
                7070,
                "localhost",
                8080,
                1);
    }
    private static Message generateMessage(String str, int followerPort)
    {
        return new Message(Message.MessageType.COMPLETED_WORK,
                str.getBytes(),
                "localhost",
                7070,
                "localhost",
                followerPort,
                1);
    }
}
