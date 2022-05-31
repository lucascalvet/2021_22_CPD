package processors.client;

import utils.MessageMulticast;
import utils.Utils;

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaveProcessor implements Runnable {
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final int NTHREADS = 2;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private int counter;
    public LeaveProcessor(String nodeId, String key, int port, PrintWriter writer, int counter){
        this.port = port;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.writer = writer;
        this.counter = counter;
    }

    @Override
    public void run() {
        // creating >> L << message
        String lMsg = nodeId + counter;

        // multicasting message
        try {
            this.threadPool.execute(new MessageMulticast(nodeId, port, ""));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
