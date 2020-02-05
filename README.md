# Zookeeper_Distributed_System
Created a fault-tolerant, distributed system from scratch in java, that:

* Implemented the ZooKeeper election algorithm for leader election 
*  Ensured each server remained up to date regarding which servers were up and which were down, using a system of heart beats and gossip messages via the UDP protocol 
*   Used HTTP to provide communication between the client and the gateway and used TCP to pass work information throughout the cluster (from the gateway, to the leader, to the worker servers and back)
