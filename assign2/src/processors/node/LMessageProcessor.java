package processors.node;

import protocol.Node;
import utils.Utils;

public class LMessageProcessor implements Runnable{
    private final int counter;
    private final String nodeId;
    private final Node node;
    public LMessageProcessor(Node node, String nodeId, int counter) {
        this.counter = counter;
        this.nodeId = nodeId;
        this.node = node;
    }

    @Override
    public void run() {
        // update membership logs
        Utils.updateLogs(nodeId, counter, node.getHashedId());
    }
}
