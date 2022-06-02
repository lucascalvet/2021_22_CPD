package processors.client.membership;

import java.io.*;
import java.net.*;

public class JoinProcessor implements Runnable {
    private int initializationTcpPort = 590; //TODO: check for open ports and use one of them
    private int counter;
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private PrintWriter writer;
    private String nodeId;
    private Integer timeout = 5000;

    public JoinProcessor(PrintWriter writer, int counter, InetAddress multicastAddress, Integer multicastPort, String nodeId) {
        this.writer = writer;
        this.counter = counter;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
    }

    @Override
    public void run() {
        // outer loop that is used for the retransmissions
        for (int r = 0; r < 3; r++) {
            // multicast >> J << message
            String jMessage =  nodeId + " " + counter + " " + initializationTcpPort;

            DatagramSocket datagramSocket;
            byte[] buf;

            try {
                datagramSocket = new DatagramSocket();
                buf = jMessage.getBytes();

                DatagramPacket packet = null;
                packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(multicastAddress.getHostName()), multicastPort);

                datagramSocket.send(packet);
                datagramSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // inner loop used to accept the 3 tcp connections
            try {
                for (int a = 0; a < 3; a++) {
                    ServerSocket serverSocket = null;

                    serverSocket = new ServerSocket(initializationTcpPort);

                    try {
                        serverSocket.setSoTimeout(timeout);
                    }catch (SocketException e){
                        break;
                    }

                    Socket socket = serverSocket.accept();
                    System.out.println("New Socket to receive initialization logs");

                    // read one line at time of input (because logs have multiple lines commands are only one line)
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                    String line = "";
                    while ("end".equals(line)) {
                        line = reader.readLine();
                    }

                    if (a == 2) {
                        // TODO: code to check for most recent log and create them
                        return; // needed to avoid going to the code that assumes that node is alone
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

// for i to 3 retransmits
//   for i to 3 accepted connections
//       return
//
// code to assume node is alone here!

