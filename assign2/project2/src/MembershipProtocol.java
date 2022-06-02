/** INTER NODE MESSAGES FORMAT **/

// JOIN       (size: 4) -> J <node_id> <node_counter> <port_to_receive_logs>
// LEAVE      (size: 3) -> L <node_id> <node_counter>
// MEMBERSHIP (size: -) -> M <node_id> /n LOGS /n logs_file (each line is a log file line) /n MEMBERS /n members_list (each line is a member)
// UPDATE     (size:  ) -> U <node_id> /n LOGS /n logs_file


/** MESSAGES FROM TEST CLIENT **/

// node receives "join" from client -> accept connections in port
//                                  -> multicast "J" to other nodes
//                                  -> upon accepting 3 connections in port, stop accepting more of them
//                                  -> if it does not receive 3 connections:
//                                          -> retransmit the "J" to a total of 3 times including the first
//                                  -> if it receives crete membership logs and members list

// node receives "leave" from client -> multicast "L" to other nodes TODO


/** MESSAGES FROM OTHER NODES **/

// node receives "J" from node -> update membership log with <node id> and <membership counter> received
//                             -> if node is not in members list or if there are changes in the message:
//                                  -> update members list adding node id
//                                  -> !!some nodes!! do the initialization process:
//                                                                         -> waits for a random time length
//                                                                         -> send "M": members list and 32 most recent logs (Through TCP)

// node receives "L" from node -> update membership log with <node id> and <membership counter> received
//                             -> update members list removing node id

// node receives "U" from node -> update the membership logs
//


/** OTHER PROTOCOL MECHANICS **/

// This !!some nodes!! have to be updated -> how to find the ones updated?

// on every 1 second one node should broadcast the most recent 32 logs (messages of type 'U') -> prevent outdated nodes from doing this updates

// the membership log should be keep with only one log for node -> the node with the largest counter
//                                                              -> do a cleaning function

import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipProtocol implements Runnable {
    private final Integer storePort;
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private final int NTHREADS = 10;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;
    private final String invalidMessage = "InvalidMessage";
    private InetAddress inetAddress;
    private int counter = 0;
    private String nodeId;

    MembershipProtocol(String nodeId, InetAddress multicastAddress, Integer multicastPort, Integer storePort) throws UnknownHostException {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.storePort = storePort;
        this.inetAddress = InetAddress.getByName(nodeId);
        this.nodeId = nodeId;
    }

    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        // socket to receive commands join and leave from client
        try {
            this.threadPool.execute(new MembershipClient(inetAddress, multicastAddress, multicastPort, storePort, counter, nodeId));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        // multicast socket to receive inter node multicasted messages


        this.threadPool.shutdown();
    }
}
