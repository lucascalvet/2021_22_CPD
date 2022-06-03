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
//                                  -> if it receives create membership logs and members list

// node receives "leave" from client -> multicast "L" to other nodes


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


package protocol;

import processors.node.JMessageProcessor;
import processors.node.LMessageProcessor;
import processors.node.UMessageProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipNode implements Runnable {
    private final Node node;
    private final ExecutorService threadPool;
    private boolean exit;

    MembershipNode(Node node) {
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
        System.out.println(Thread.currentThread().getName() + ": Created thread pool with " + threadCount + "threads");
        exit = false;
    }

    public void run() {
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(node.getMulticastPort());
            byte[] buf = new byte[1024];
            socket.joinGroup(node.getMulticastAddr());

            while (!exit) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                if (exit) break;
                String received = new String(
                        packet.getData(), packet.getOffset(), packet.getLength());

                if (received.isEmpty()) continue;

                char msgId = received.charAt(0);

                String[] message = received.split("\\s+");
                String nodeId;
                int counter, port;

                switch (msgId) {
                    // receives a J message
                    case 'J':
                        if (message.length != 4) {
                            continue;
                        }

                        nodeId = message[1];

                        try {
                            counter = Integer.parseInt(message[2]);
                            port = Integer.parseInt(message[3]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new JMessageProcessor(node, nodeId, port, counter));
                        break;
                    // receives a L message
                    case 'L':
                        if (message.length != 3) {
                            continue;
                        }

                        nodeId = message[1];
                        try {
                            counter = Integer.parseInt(message[2]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new LMessageProcessor(node, nodeId, counter));
                        break;
                    // receives a M message
                    case 'U':
                        nodeId = message[1];
                        // TODO: missing getting/parsing membership logs
                        this.threadPool.execute(new UMessageProcessor(nodeId));
                        break;
                    // ignore wrong messages
                    default:
                        continue;
                }
            }

        } catch (Exception e) {
            try {
                assert socket != null;
                socket.leaveGroup(node.getMulticastAddr());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            socket.close();
            this.threadPool.shutdown();
        }
    }

    public void stop() {
        this.threadPool.shutdown();
        this.exit = true;
    }
}
