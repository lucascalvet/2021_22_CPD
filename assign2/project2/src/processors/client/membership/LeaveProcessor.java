package processors.client.membership;

import processors.client.store.PutProcessor;
import protocol.Node;
import utils.MessageSender;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaveProcessor implements Runnable {
    private Node node;
    private final ExecutorService threadPool;
    public LeaveProcessor(Node node) {
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public void run() {
        //Storage change before membership message is transmitted
        File dir = new File(Utils.BASE_DIR + node.getHashedId() + File.separator + "storage");
        File[] storageListing = dir.listFiles();
        if (storageListing != null) {
            for (File child : storageListing) {
                String key = child.getName().replaceFirst("[.][^.]+$", "");
                String value = Utils.getFileContent(node.getHashedId() + File.separator + "storage" + File.separator + child.getName());
                try {
                    threadPool.execute(new MessageSender(Utils.getActiveMembersSorted(node.getHashedId(), key).get(0), node.getStorePort(), "P 1 " + value));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Utils.writeToFile(node.getHashedId() + File.separator + "storage" + File.separator + child.getName(), Utils.MSG_TOMBSTONE, false);
            }
        }

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
