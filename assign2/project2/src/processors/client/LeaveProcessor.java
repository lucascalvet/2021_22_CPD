package processors.client;

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
    public LeaveProcessor(PrintWriter writer, int counter, InetAddress multicastAddress, Integer multicastPort) {
        this.writer = writer;
        this.counter = counter;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    @Override
    public void run() {
        // creating >> L << message
        //String lMsg = nodeId + counter;

        // multicasting message
        String jMessage = null;
        DatagramSocket socket;
        InetAddress group;
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
