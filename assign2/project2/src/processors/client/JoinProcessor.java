package processors.client;

import java.io.*;
import java.net.*;

public class JoinProcessor implements Runnable {
    private int initializationTcpPort = 590;
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private PrintWriter writer;
    private String nodeId;

    public JoinProcessor(PrintWriter writer, InetAddress multicastAddress, Integer multicastPort, String nodeId) {
        this.writer = writer;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
    }

    @Override
    public void run() {
        // accept connections
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(initializationTcpPort);
            Socket socket = serverSocket.accept();
            System.out.println("New Socket to receive initialization logs");

            // read one line at time of input (because logs have multiple lines commands are only one line)
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String line = "";
            while("end".equals(line)) {
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // multicast >> J << message
        String jMessage =  nodeId + " " + counter + " " + initializationTcpPort;

        DatagramSocket socket;
        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = jMessage.getBytes();

            DatagramPacket packet = null;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(multicastAddress.getHostName()), multicastPort);

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


for(int r = 0; r < 3; r++){
    for(int a = 0; a < 3; a++){

    }
}

// for i to 3 retransmits
//   for i to 3 accepted connections
//       return
//
// code to assume node is alone here!