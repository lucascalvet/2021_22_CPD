/** INTER NODE MESSAGES FORMAT **/

// JOIN       (size: 4) -> J <node_id> <node_counter> <port_to_receive_logs>
// LEAVE      (size: 3) -> L <node_id> <node_counter>
// MEMBERSHIP (size: -) -> M <node_id> /n LOGS /n logs_file (each line is a log file line) /n MEMBERS /n members_list (each line is a member)


/** MESSAGES FROM TEST CLIENT **/

// node receives "join" from client -> accept connections in port
//                                  -> multicast "J" to other nodes
//                                  -> upon accepting 3 connections in port, stop accepting more of them
//                                  -> if it does not receive 3 connections:
//                                          -> retransmit the "J" to a total of 3 times including the first
//                                  -> if it receives crete membership logs and members list

// node receives "leave" from client -> multicast "L" to other nodes


/** MESSAGES FROM OTHER NODES **/

// node receives "J" from node -> update membership log with <node id> and <membership counter> received
//                             -> if node is not in members list or if there is and there are changes in the message:
//                                  -> update members list adding node id
//                                  -> !!some nodes!! do the initialization process:
//                                                                         -> waits for a random time length
//                                                                         -> send "M": members list and 32 most recent logs (Through TCP)
//                             -> else if node is not in members list:
//                                  -> ignore "J" from that node
// node receives "L" from node -> update membership log with <node id> and <membership counter> received
//                             -> update members list removing node id

// node receives "M" from node -> update the membership logs
//                             -> prevent outdated nodes from doing this updates


/** OTHER PROTOCOL MECHANICS **/

// This !!some nodes!! have to be updated -> how to find the ones updated?

// on every 1 second one node should broadcast the most recent 32 logs

// the membership log should be keep with only one log for node -> the node with the largest counter
//                                                              -> do a cleaning function

import processors.client.JoinProcessor;
import processors.client.LeaveProcessor;
import processors.node.JMessageProcessor;
import processors.node.LMessageProcessor;
import processors.node.MMessageProcessor;
import utils.AccessPoint;
import utils.Utils;

import java.io.*;
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
    private int counter = 0;

    MembershipProtocol(InetAddress multicastAddress, Integer multicastPort, Integer storePort){
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.storePort = storePort;
    }

    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        // socket to receive commands join and leave from client
        try (ServerSocket serverSocket = new ServerSocket(storePort)) {
            Socket socket = serverSocket.accept();
            System.out.println("New Socket for commands join and leave from client");

            // read one line of input (because client commands are only one line)
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String commandLine = reader.readLine();
            String[] message = commandLine.split("\\s+");

            // create output stream to answer client with command status
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            // parse command inputted from client
            if(message.length != 1){
                writer.println(invalidMessage);
                throw new IllegalArgumentException("Wrong number of arguments given");
            }
            String operation = message[0];

            switch(operation) {
                case "join":
                    this.threadPool.execute(new JoinProcessor(writer, counter, multicastAddress, multicastPort));
                    break;
                case "leave":
                    this.threadPool.execute(new LeaveProcessor(writer, counter, multicastAddress, multicastPort));
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // multicast socket to receive inter node multicasted messages
        String multicastMessage = "test";
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(4446);
            byte[] buf = new byte[256];

            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if ("end".equals(received)) {
                    break;
                }
            }

            // checks for valid arguments
            if(msgId.equals("J") || msgId.equals("L") || msgId.equals("M")){
                writer.println(invalidMessage);
            }

            switch (msgId){
                // receives a J message
                case "J":
                    if(message.length != 4){
                        writer.println(invalidMessage);
                        return;
                    }
                    if(counter != 0) counter++;

                    this.threadPool.execute(new JMessageProcessor(nodeId, opArg, port, writer, message, counter));
                    break;
                // receives a L message
                case "L":
                    if(message.length != 3){
                        writer.println(invalidMessage);
                        return;
                    }
                    counter--;

                    this.threadPool.execute(new LMessageProcessor(nodeId, opArg, port, writer, message, counter));
                    break;
                // receives a M message
                case "M":
                    this.threadPool.execute(new MMessageProcessor(nodeId, opArg, port, writer, message, counter, reader));
                    break;
                // invalid message
                case invalidMessage:
                    throw new RuntimeException(invalidMessage);
                    // message not recognized
                default:
                    writer.println(invalidMessage);
            }

            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        this.threadPool.shutdown();
    }
}
