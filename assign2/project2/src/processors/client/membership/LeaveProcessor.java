package processors.client.membership;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LeaveProcessor implements Runnable {
    private int counter;
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private PrintWriter writer;
    private String nodeId;
    public LeaveProcessor(PrintWriter writer, int counter, InetAddress multicastAddress, Integer multicastPort, String nodeId) {
        this.writer = writer;
        this.counter = counter;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
    }

    @Override
    public void run() {
        // creating >> L << message
        String lMessage = nodeId + counter;

        // multicasting message
        DatagramSocket socket;
        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = lMessage.getBytes();

            DatagramPacket packet = null;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(multicastAddress.getHostName()), multicastPort);

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
