package processors.client.membership;

import protocol.Node;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class LeaveProcessor implements Runnable {
    private Node node;
    public LeaveProcessor(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        // creating >> L << message
        String lMessage = node.getNodeId() + node.getCounter();

        // multicasting message
        DatagramSocket socket;
        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = lMessage.getBytes();

            DatagramPacket packet = null;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
