package processors.client.membership;

import protocol.MembershipNode;
import protocol.Node;

import java.io.*;
import java.net.*;
import java.util.*;

public class JoinProcessor implements Runnable {
    private final Node node;
    private final Socket clientSocket;
    private final MembershipNode membershipNode;
    private String result = "";

    public JoinProcessor(Node node, MembershipNode membershipNode, Socket clientSocket) {
        this.node = node;
        this.clientSocket = clientSocket;
        this.membershipNode = membershipNode;
    }

    public JoinProcessor(Node node, MembershipNode membershipNode) {
        this.node = node;
        this.clientSocket = null;
        this.membershipNode = membershipNode;
    }

    public String getResult() {
        return result;
    }

    @Override
    public void run() {

        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int nReceived = 0;
        List<String> receivedFrom = new ArrayList<>();
        Map<String, Integer> nodes = new LinkedHashMap<>();
        PrintWriter clientWriter = null;
        if (clientSocket != null) {
            try {
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (node.getCounter() % 2 == 0) {
            if (clientWriter != null) {
                clientWriter.println(node.getNodeId() + " JOIN-> Node is already joined! Aborting.");
                clientWriter.println(node.getMSG_END_SERVICE());
            }
            result = node.getNodeId() + " JOIN-> Node is already joined! Aborting.";
            return;
        }

        this.node.setCounter();

        String info = "Initializing join process.";
        System.out.println(info);
        if (clientWriter != null)
            clientWriter.println(node.getNodeId() + " JOIN-> " + info);

        boolean error = false;
        // multicast >> J << message
        String jMessage = "J " + node.getNodeId() + " " + node.getCounter() + " " + serverSocket.getLocalPort();

        for (int r = 0; nReceived < 3 && r < 3; r++) {

            DatagramSocket datagramSocket;
            byte[] buf;

            try {
                datagramSocket = new DatagramSocket();
                buf = jMessage.getBytes();

                DatagramPacket packet;
                packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

                datagramSocket.send(packet);
                datagramSocket.close();
            } catch (IOException e) {
                result = node.getNodeId() + " JOIN-> Error initializing node.";
                throw new RuntimeException(e);
            }

            try {
                while (nReceived < 3) {

                    try {
                        serverSocket.setSoTimeout(2000);
                    } catch (SocketException e) {
                        System.out.println("Error setting socket timeout. " + e.getMessage());
                        error = true;
                        break;
                    }

                    Socket socket;
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timed out. Retransmitting " + (2 - r) + " more times...");
                        break;
                    }
                    System.out.println("Accepted connection to receive initialization logs");

                    // read one line at time of input (because logs have multiple lines commands are only one line)
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String line;
                    try {
                        line = reader.readLine();
                    } catch (IOException e) {
                        error = true;
                        continue;
                    }
                    if (line == null) {
                        System.out.println("Empty message!");
                        continue;
                    }

                    String[] message = line.split("\\s+");
                    if (message.length != 2 || message[0].isEmpty() || message[0].charAt(0) != 'M') {
                        System.out.println("Received unexpected message in join logs TCP port.");
                        continue;
                    }
                    String receivedNodeId = message[1];

                    if (receivedFrom.contains(receivedNodeId)) {
                        System.out.println("Skipping logs from node already received");
                        continue;
                    }

                    while (true) {
                        try {
                            line = reader.readLine();
                        } catch (IOException e) {
                            error = true;
                            nReceived--;
                            break;
                        }
                        System.out.println("Got logs from other node");
                        if (line == null || "end".equals(line)) break;
                        String[] split = line.split("\\s+");
                        if (split.length == 2) {
                            try {
                                String nodeId = split[0];
                                int nodeCounter = Integer.parseInt(split[1]);
                                if (nodes.containsKey(nodeId)) {
                                    if (nodes.get(nodeId) <= nodeCounter) {
                                        nodes.remove(nodeId);
                                        nodes.put(nodeId, nodeCounter);
                                    }
                                } else {
                                    nodes.put(nodeId, nodeCounter);
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Error parsing membership message from " + socket.getRemoteSocketAddress().toString() + ", while reading counter. Got" + split[1]);
                            }
                        } else {
                            System.out.println("Error parsing membership message from " + socket.getRemoteSocketAddress().toString() + ". Wrong number of attributes in log line");
                        }
                        receivedFrom.add(receivedNodeId);
                    }
                    nReceived++;
                }
            } catch (IOException e) {
                result = node.getNodeId() + " JOIN-> Error initializing node.";
                throw new RuntimeException(e);
            }
        }

        this.node.setAllLogs(nodes);

        if (error) {
            info = "Failed to join the node";
        } else {
            info = "Joined node successfully. Received logs from " + nReceived + " nodes.";
        }
        System.out.println(info);
        if (clientWriter != null) {
            clientWriter.println(node.getNodeId() + " JOIN-> " + info);
            clientWriter.println(node.getMSG_END_SERVICE());
        }
        result = node.getNodeId() + " JOIN-> " + info;

        System.out.println("Starting multicast thread.");

        new Thread(membershipNode, "Multicast Thread").start();
    }
}
