
import processors.node.JMessageProcessor;
import processors.node.LMessageProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipNode implements Runnable {
    private final Integer storePort;
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private final int NTHREADS = 10;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;
    private final String invalidMessage = "InvalidMessage";
    private InetAddress inetAddress;

    MembershipNode(String nodeId, InetAddress multicastAddress, Integer multicastPort, Integer storePort) throws UnknownHostException {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.storePort = storePort;
        this.inetAddress = InetAddress.getByName(nodeId);
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        String multicastMessage = "test";
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(multicastPort);
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

                switch (msgId) {
                    // receives a J message
                    case 'J':
                        if (message.length != 4) {
                            continue;
                        }

                        nodeId = message[1];

                        int counter, port;
                        try {
                            counter = Integer.parseInt(message[2]);
                            port = Integer.parseInt(message[3]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new JMessageProcessor(nodeId, port, counter));
                        break;
                    // receives a L message
                    case 'L':
                        if (message.length != 3) {
                            continue;
                        }

                        nodeId = message[1];
                        try {
                            int counter = Integer.parseInt(message[2]);
                        }
                        catch (NumberFormatException e) {
                            continue;
                        }

                        this.threadPool.execute(new LMessageProcessor(nodeId, opArg, port, writer, message, counter));
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