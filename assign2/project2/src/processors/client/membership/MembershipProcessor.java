package processors.client.membership;

import protocol.Node;
import utils.Utils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MembershipProcessor implements Runnable {
    private final Node node;
    private int silenceCounter;

    public MembershipProcessor(Node node) {
        this.node = node;
        this.silenceCounter = 0;
    }

    @Override
    public void run() {
            // Test if node should transmit periodic membership message
            if (silenceCounter > 1) {
                transmit();
            }
            this.increaseSilenceCounter();
    }

    public synchronized void increaseSilenceCounter() {
        this.silenceCounter++;
    }

    public synchronized void resetSilenceCounter() {
        this.silenceCounter = 0;
    }

    public void transmit() {
        System.out.println("Transmitting periodic membership message...");
        // multicast >> M << message
        String toSend = node.get32Logs();
        String mMessage = "M " + node.getNodeId() + "\n" + toSend + "end";

        DatagramSocket datagramSocket;
        byte[] buf;
        try {
            datagramSocket = new DatagramSocket();
            buf = mMessage.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

            datagramSocket.send(packet);
            datagramSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

// for i to 3 retransmits
//   for i to 3 accepted connections
//       return
//
// code to assume node is alone here!

