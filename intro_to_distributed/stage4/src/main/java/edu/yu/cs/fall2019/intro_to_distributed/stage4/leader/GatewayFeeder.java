package edu.yu.cs.fall2019.intro_to_distributed.stage4.leader;

import edu.yu.cs.fall2019.intro_to_distributed.Message;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util;
import edu.yu.cs.fall2019.intro_to_distributed.util.Util2;
import edu.yu.cs.fall2019.intro_to_distributed.stage4.gateway.GateWay;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Take messages wholesale from the FOLLOWERS and send them straight to the
 * gateway.
 * <p>
 * While do this, mark off (by deleting) that that server completed it's work
 */
public class GatewayFeeder implements Runnable
{

    private final Map<Long, List<Message>> followersWork;
    private final LinkedBlockingQueue<Message> messagesFromAllFollowers;
    private final long GATEWAY_PORT = GateWay.GATEWAY_PORT;
    private final Map<Long, LinkedBlockingQueue<Message>> followerOutgoing;
    private final LinkedBlockingQueue<Message> gatewayOutGoing;

    private volatile boolean shutdown = false;

    public GatewayFeeder(Map<Long, List<Message>> followersWork, LinkedBlockingQueue<Message> messagesFromAllFollowers, long GATEWAY_PORT, Map<Long, LinkedBlockingQueue<Message>> followerOutgoing)
    {
        this.followersWork = followersWork;
        this.messagesFromAllFollowers = messagesFromAllFollowers;
        this.followerOutgoing = followerOutgoing;
        this.gatewayOutGoing = followerOutgoing.get(this.GATEWAY_PORT);//todo - turn this back on for production
        //this.gatewayOutGoing = new LinkedBlockingQueue<>(); //todo - this is only for testing
    }

    public void shutdown()
    {
        Util2.printReplace("Shutting down gatewayFeeder", 905);
        shutdown = true;
    }

    @Override
    public void run()
    {
        //take a message out of the messages from all followers
        //remove that message from that FOLLOWER's queue
        //move that message to the Gateway queue
        while (!shutdown) {
            try {
                Message messageToSend = this.messagesFromAllFollowers.poll(2, TimeUnit.SECONDS);
                if (messageToSend == null) {
                    Thread.yield();
                    continue;
                }
                long id = messageToSend.getRequestID();
                long port = messageToSend.getSenderPort(); //FOLLOWER port
                List<Message> messages = followersWork.get(port);
                if (messages != null) { //shouldn't be null, but just in case I guess
                    for (Message m : messages) {
                        long otherID = m.getRequestID();
                        if (id == otherID) messages.remove(m);
                        break;
                    }
                    gatewayOutGoing.put(messageToSend);
                    Util2.printReplace("Sending back to the Gateway, message with request id = "+ messageToSend.getRequestID()
                            +" from FOLLOWER port:"+messageToSend.getSenderPort()
                            + " via LEADER port:"+ messageToSend.getReceiverPort(), 13_01);
                    Util2.printReplace("))))GATEWAY FEEDER(((( Moving a message to the gateway, message:" + new String(messageToSend.getMessageContents()), 905);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Util2.printReplace("Got the end of the [GATEWAY FEEDER], so turning off ", 770);
    }
}
