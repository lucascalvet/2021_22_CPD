package processors.node;

import protocol.Node;

import java.util.HashMap;
import java.util.Map;

public class MMessageProcessor implements Runnable{
    private final Node node;
    private final String received;

    public MMessageProcessor(Node node, String received) {
        this.node = node;
        this.received = received;
    }

    @Override
    public void run() {
        Map<String, Integer> receivedLogs = new HashMap<String, Integer>();
        String[] logs = received.split("\n");

        for (int i = 1; i < logs.length; i++) {
            String line = logs[i];
            String[] log = line.split("\\s+");
            if (log.length != 2) {
                continue;
            }
            String nodeId = log[0];
            int nodeCounter;
            try {
                nodeCounter = Integer.parseInt(log[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            receivedLogs.put(nodeId, nodeCounter);
        }
        node.updateAllLogs(receivedLogs);
    }
}
