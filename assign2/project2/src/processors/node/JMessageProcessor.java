package processors.node;

import utils.Utils;

import java.io.PrintWriter;

public class JMessageProcessor implements Runnable{
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final int NTHREADS = 2;
    private int counter;
    private String[] message;
    public JMessageProcessor(String nodeId, String key, int port, PrintWriter writer, String[] message, int counter) {
        this.port = port;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.writer = writer;
        this.message = message;
        this.counter = counter;
    }

    @Override
    public void run() {
        // TODO
        // update membership log
        String newLog = message[1] + message[2];
    }
}
