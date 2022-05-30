package processors.node;

import utils.Utils;

import java.io.BufferedReader;
import java.io.PrintWriter;

public class MMessageProcessor implements Runnable{
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final int NTHREADS = 2;
    private String[] command;
    private BufferedReader messageReader;
    private int counter;
    public MMessageProcessor(String nodeId, String key, int port, PrintWriter writer, String[] command, int counter, BufferedReader reader) {
        this.port = port;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.writer = writer;
        this.command = command;
        this.messageReader = reader;
        this.counter = counter;
    }

    @Override
    public void run() {
        // TODO

        // update the membership logs
        Utils.updateLogs();
    }
}
