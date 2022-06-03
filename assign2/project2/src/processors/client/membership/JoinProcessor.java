package processors.client.membership;

import protocol.Node;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class JoinProcessor implements Runnable {
    private final Node node;

    public JoinProcessor(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int nReceived = 0;
        Map<String, Integer> nodes = new HashMap<String, Integer>();
        // outer loop that is used for the retransmissions
        for (int r = 0; nReceived < 3 && r < 3; r++) {
            // multicast >> J << message
            String jMessage =  node.getNodeId() + " " + node.getCounter() + " " + serverSocket.getLocalPort();

            DatagramSocket datagramSocket;
            byte[] buf;

            try {
                datagramSocket = new DatagramSocket();
                buf = jMessage.getBytes();

                DatagramPacket packet = null;
                packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

                datagramSocket.send(packet);
                datagramSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // inner loop used to accept the 3 tcp connections
            try {
                while (nReceived < 3) {

                    try {
                        serverSocket.setSoTimeout(3000);
                    }catch (SocketException e){
                        System.out.println("Error setting socket timeout. " + e.getMessage());
                        break;
                    }

                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                    }
                    catch (SocketTimeoutException e) {
                        System.out.println("Socket timed out. Retransmitting " + (2 - r) + " more times...");
                        break;
                    }
                    System.out.println("New Socket to receive initialization logs");

                    // read one line at time of input (because logs have multiple lines commands are only one line)
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String line = "";
                    while (true) {
                        try {
                            line = reader.readLine();
                        }
                        catch (IOException e) {
                            nReceived--;
                            break;
                        }
                        if (line == null || "end".equals(line)) break;
                        String[] splited = line.split("\\s+");
                        if (splited.length == 2) {
                            try {
                                String nodeId = splited[0];
                                int nodeCounter = Integer.parseInt(splited[1]);
                                if (nodes.containsKey(nodeId)) {
                                    if (nodes.get(nodeId) < nodeCounter) {
                                        nodes.put(nodeId, nodeCounter);
                                    }
                                }
                                else {
                                    nodes.put(nodeId, nodeCounter);
                                }
                            }
                            catch (NumberFormatException e) {
                                System.out.println("Error parsing membership message from " + socket.getRemoteSocketAddress().toString() + ", while reading counter. Got" + splited[1]);
                            }
                        }
                        else {
                            System.out.println("Error parsing membership message from " + socket.getRemoteSocketAddress().toString() + ". Wrong number of attributes in log line");
                        }
                    }
                    nReceived++;
                    if (nReceived >= 3) break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.node.setAllLogs(nodes);
    }
}

// for i to 3 retransmits
//   for i to 3 accepted connections
//       return
//
// code to assume node is alone here!

