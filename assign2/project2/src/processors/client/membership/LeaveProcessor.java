package processors.client.membership;

import protocol.MembershipNode;
import protocol.Node;
import utils.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class LeaveProcessor implements Runnable {
    private Node node;
    private final Socket clientSocket;
    private final MembershipNode membershipNode;
    public LeaveProcessor(Node node, Socket clientSocket, MembershipNode membershipNode) {
        this.node = node;
        this.node.setCounter();
        this.clientSocket = clientSocket;
        this.membershipNode = membershipNode;
    }

    @Override
    public void run() {
        PrintWriter clientWriter;
        try {
            clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (node.getCounter() % 2 != 0) {
            clientWriter.println("Node isn't joined to any cluster! Aborting.");
            clientWriter.println(Utils.MSG_END_SERVICE);
            return;
        }

        clientWriter.println("Quitting multicast thread.");

        this.membershipNode.stop();

        clientWriter.println("Initializing leave process.");

        node.setCounter();

        // creating >> L << message
        String lMessage = node.getNodeId() + node.getCounter();

        // multicasting message
        DatagramSocket socket;
        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = lMessage.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        clientWriter.println("Finished leave process.");
        clientWriter.println(Utils.MSG_END_SERVICE);
    }
}
