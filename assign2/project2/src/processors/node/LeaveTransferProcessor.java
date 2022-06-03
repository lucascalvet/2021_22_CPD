package processors.node;

import protocol.Node;
import utils.MessageSender;

import java.net.UnknownHostException;


public class LeaveTransferProcessor implements Runnable{
    private final Node node;
    private final String key;
    private final String value;

    public LeaveTransferProcessor(Node node, String key){
        this.node = node;
        this.key = key;
        this.value = node.getValue(this.key);
    }

    @Override
    public void run() {
        try {
            MessageSender messenger = new MessageSender(node.getActiveMembersSorted(key).get(0), node.getStorePort(), "P " + String.valueOf(node.getREPLICATION_FACTOR() + 1) + " " + value);
            messenger.run();
            //System.out.println(messenger.getAnswer());
            node.tombstone(key);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
