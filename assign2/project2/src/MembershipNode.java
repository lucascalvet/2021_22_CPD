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
    private int counter = 0;

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
            socket = new MulticastSocket(4446);
            byte[] buf = new byte[256];

            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(
                        packet.getData(), 0, packet.getLength());
                if ("end".equals(received)) {
                    break;
                }
            }

            // checks for valid arguments
            if(msgId.equals("J") || msgId.equals("L") || msgId.equals("M")){
                writer.println(invalidMessage);
            }

            switch (msgId){
                // receives a J message
                case "J":
                    if(message.length != 4){
                        writer.println(invalidMessage);
                        return;
                    }
                    if(counter != 0) counter++;

                    this.threadPool.execute(new JMessageProcessor(nodeId, opArg, port, writer, message, counter));
                    break;
                // receives a L message
                case "L":
                    if(message.length != 3){
                        writer.println(invalidMessage);
                        return;
                    }
                    counter--;

                    this.threadPool.execute(new LMessageProcessor(nodeId, opArg, port, writer, message, counter));
                    break;
                // receives a M message
                case "M":
                    this.threadPool.execute(new MMessageProcessor(nodeId, opArg, port, writer, message, counter, reader));
                    break;
                // invalid message
                case invalidMessage:
                    throw new RuntimeException(invalidMessage);
                    // message not recognized
                default:
                    writer.println(invalidMessage);
            }

            socket.leaveGroup(group);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.threadPool.shutdown();
    }
}
