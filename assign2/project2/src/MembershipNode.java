
import processors.node.JMessageProcessor;
import processors.node.LMessageProcessor;
import processors.node.MMessageProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipNode implements Runnable {
    private final Node node;
    private final ExecutorService threadPool;
    private InetAddress inetAddress;

    MembershipNode(Node node) throws UnknownHostException {
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
        System.out.println(Thread.currentThread().getName() + ": Created thread pool with " + threadCount + "threads");
    }

    public void run() {

        String multicastMessage = "test";
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(node.getMulticastPort());
            byte[] buf = new byte[1024];

            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

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

                        this.threadPool.execute(new JMessageProcessor(this.node, nodeId, port, counter));
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
                    case 'M':
                        this.threadPool.execute(new MMessageProcessor(nodeId, opArg, port, writer, message, counter, reader));
                        break;
                    // invalid message
                    case invalidMessage:
                        throw new RuntimeException(invalidMessage);
                        // message not recognized
                    default:
                        continue;
                }
            }


            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.threadPool.shutdown();
    }
}