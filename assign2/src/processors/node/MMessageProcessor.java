package processors.node;

import processors.client.membership.MembershipProcessor;
import protocol.Node;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MMessageProcessor implements Runnable{
    private final Node node;
    private final String nodeId;
    private final String received;
    private final MembershipProcessor membershipProcessor;

    public MMessageProcessor(Node node, String nodeId, String received, MembershipProcessor membershipProcessor) {
        this.node = node;
        this.nodeId = nodeId;
        this.received = received;
        this.membershipProcessor = membershipProcessor;
    }

    @Override
    public void run() {
        Map<String, Integer> receivedLogs = new LinkedHashMap<>();
        String[] logs = received.split("\n");

        for (int i = 1; i < logs.length; i++) {
            String line = logs[i];

            String[] log = line.split("\\s+");

            if ("end".equals(line)) break;

            if (log.length != 2) {
                System.out.println("Error parsing membership message from " + nodeId + ". Wrong number of attributes in log line");
                continue;
            }
            String nodeId = log[0];
            int nodeCounter;
            try {
                nodeCounter = Integer.parseInt(log[1]);
            } catch (NumberFormatException e) {
                System.out.println("Error parsing membership message from " + nodeId + ", while reading counter. Got" + log[1]);
                continue;
            }

            receivedLogs.put(nodeId, nodeCounter);
        }
        List<Boolean> updates = node.updateAllLogs(receivedLogs);

        // If all log entries caused an update (node has probably crashed and is now outdated more than the 32 logs give), reconnect node
        if (updates.get(2)) {
            node.setNeedsReconnect(true);
        }

        // If received log was outdated, broadcast periodic membership
        if (updates.get(1)) {
            membershipProcessor.transmit();
            membershipProcessor.increaseSilenceCounter();
        }
    }
}
