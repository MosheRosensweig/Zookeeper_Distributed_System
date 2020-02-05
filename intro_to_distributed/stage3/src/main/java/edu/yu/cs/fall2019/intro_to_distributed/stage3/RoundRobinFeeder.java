package edu.yu.cs.fall2019.intro_to_distributed.stage3;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.Util2;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Take messages from the Gateway and pass them to a FOLLOWER
 *
 * NOTE: I don't change the message sender port, becuase the FOLLOWER
 * should have the leader port just with the TCP connection itself
 */
public class RoundRobinFeeder implements Runnable
{
    private final LinkedBlockingQueue<Message> messagesFromGateway;
    private final Map<Long, LinkedBlockingQueue<Message>> workerOutgoingQueues;
    private Map<Long, List<Message>> followersWork;

    private volatile boolean shutdown = false;


    public RoundRobinFeeder(LinkedBlockingQueue<Message> messagesFromGateway, Map<Long, LinkedBlockingQueue<Message>> workerOutgoingQueues, Map<Long,List<Message>> followersWork)
    {
        this.messagesFromGateway = messagesFromGateway;
        this.workerOutgoingQueues = workerOutgoingQueues;
        this.followersWork = followersWork;
    }

    public void shutdown()
    {
        Util2.printReplace("Shutting down round robin feeder");
        shutdown = true;
    }

    @Override
    public void run()
    {
        /**
         * [1] Figure out the next FOLLOWER to give work to
         * [2] poll on the messagesFromGateway
         * [3] put the message in that worker's queue
         */
        while (!shutdown) {
            Set<Long> followerPorts = getFollowerPorts();
            for (Long port : followerPorts) {
                if(port.equals((long)GateWay.GATEWAY_PORT)) continue;
                LinkedBlockingQueue<Message> follower = workerOutgoingQueues.get(port);
                if (follower == null) continue; //if that follower no longer exists
                synchronized (follower) {
                    try {
                        if (follower == null) continue; //if it disappeared
                        Message messageToSend = null;
                        while (messageToSend == null && !shutdown) {
                            messageToSend = this.messagesFromGateway.poll(2, TimeUnit.SECONDS);
                            if(messageToSend == null) Thread.yield();
                        }
                        if(messageToSend != null) { //if it's shutdown, then dont execute this
                            addToFollowerQueue(port, messageToSend);
                            follower.put(messageToSend);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        Util2.printReplace("Got the end of the [RoundRobinFEEDER], so turning off ", 770);
    }

    private HashSet<Long> getFollowerPorts()
    {
        Iterator<Long> it = workerOutgoingQueues.keySet().iterator();
        HashSet<Long> ports = new HashSet<>();
        while (it.hasNext()){
            ports.add(it.next());
        }
        return ports;
    }

    private void addToFollowerQueue(long port, Message message)
    {
        List<Message> workload = followersWork.get(port);
        if(workload == null){
            workload = new ArrayList<>();
            followersWork.put(port, workload);
        }
        workload.add(message);
    }
}
