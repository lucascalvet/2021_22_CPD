package processors.node;

import utils.Utils;

public class UMessageProcessor implements Runnable{
    private final String nodeId;
    private final String hashedId;
    public UMessageProcessor(String nodeId) {
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
    }

    @Override
    public void run() {
        // TODO: update membership logs, by comparing logs received
    }
}
