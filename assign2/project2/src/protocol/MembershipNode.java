package protocol;

import processors.node.JMessageProcessor;
import processors.node.LMessageProcessor;
import processors.node.UMessageProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipNode implements Runnable {
    private final Node node;
    private final ExecutorService threadPool;

    MembershipNode(Node node) {
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
        System.out.println(Thread.currentThread().getName() + ": Created thread pool with " + threadCount + " threads");
    }

    public void run() {
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(node.getMulticastPort());
            byte[] buf = new byte[1024];
            socket.joinGroup(node.getMulticastAddr());

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(
                        packet.getData(), packet.getOffset(), packet.getLength());

                if (received.isEmpty()) continue;

                char msgId = received.charAt(0);

                String[] message = received.split("\\s+");
                String nodeId;
                int counter, port;

                switch (msgId) {
                    // receives a J message
                    case 'J':
                        if (message.length != 4) {
                            continue;
                        }

                        nodeId = message[1];

                        try {
                            counter = Integer.parseInt(message[2]);
                            port = Integer.parseInt(message[3]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new JMessageProcessor(node, nodeId, port, counter));
                        break;
                    // receives a L message
                    case 'L':
                        if (message.length != 3) {
                            continue;
                        }

                        nodeId = message[1];
                        try {
                            counter = Integer.parseInt(message[2]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new LMessageProcessor(nodeId, counter));
                        break;
                    // receives a M message
                    case 'U':
                        nodeId = message[1];
                        // TODO: missing getting/parsing membership logs
                        this.threadPool.execute(new UMessageProcessor(nodeId));
                        break;
                    // ignore wrong messages
                    default:
                        continue;
                }
            }

        } catch (Exception e) {
            try {
                assert socket != null;
                socket.leaveGroup(node.getMulticastAddr());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            socket.close();
            this.threadPool.shutdown();
        }
    }
}
