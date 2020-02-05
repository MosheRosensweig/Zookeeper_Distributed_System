# Zookeeper_Distributed_System
Created a fault-tolerant, distributed system from scratch in java, that:

* Implemented the ZooKeeper election algorithm for leader election 
*  Ensured each server remained up to date regarding which servers were up and which were down, using a system of heart beats and gossip messages via the UDP protocol 
*   Used HTTP to provide communication between the client and the gateway and used TCP to pass work information throughout the cluster (from the gateway, to the leader, to the worker servers and back)

This was a project that was implemented in 4 stages over the course of a semester. 

[1] Build a server side program that takes java code, compiles, runs it, and returns the sysout and syserr. Addtionally, I wrote a client to query the server, and some Junit tests to test it.

[2] Implement the Zookeeper algorithm with the assumption that there is only one election, and no new servers will be added to the cluster (the second assumption was maintained throughout the semester).

[3] Combine '1' and '2'. Meaning, run an election and then have the leader accept client requests, parse them and give work to followers in a round-robin fashion, get the completed work and return it to the client.

[4] Add Falut-tolerance. I added a gateway to the system that caches client requests and serves as the middleman between the client and the leader server. Additionally, I used an all-to-all hearbeat + gossip protocal to determine if any server went down. When a server detects that another server is unreachable, it assumes it has crashed and responds accordingly (if the leader died, run a new election).

- All server messages, such as election messages, hearbeat messages, and gossip messages are sent via UDP.
- All work messages throughout the round trip from the client to a worker and back are implemented in TCP to ensure that none of the work is lost. 
- The client talks to the gateway via HTTP.
