package processors.node;

import jdk.jshell.execution.Util;
import utils.Utils;

public class LMessageProcessor implements Runnable{
    private final int counter;
    private final String nodeId;
    private final String hashedId;
    public LMessageProcessor(String nodeId, int counter) {
        this.counter = counter;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
    }

    @Override
    public void run() {
        //TODO: update members list
        Utils.updateLogs(nodeId, counter, hashedId);
    }
}
