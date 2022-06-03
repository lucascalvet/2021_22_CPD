package processors.client.store;

import protocol.Node;
import utils.MessageSender;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class DeleteProcessor implements Runnable{
    private int replicationFactor;
    private final Node node;
    private MessageSender messenger = null;
    private final String key;
    private final Socket socket;
    private final PrintWriter writer;
    private final int REPLICATION_FACTOR = 3;
    private String message = "";

    public DeleteProcessor(Node node, String key, int replicationFactor, Socket socket) throws IOException {
        this.node = node;
        this.replicationFactor = replicationFactor;
        this.key = key;
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);


        //System.out.println("DP Key: " + key);
        //System.out.println("DP Replication Factor: " + replicationFactor);
    }

    public void run(){
        boolean deleted = false;
        List<String> activeNodesSorted = node.getActiveMembersSorted(key);
        int nextRep = replicationFactor;
        if(node.pairExists(key)){
            nextRep -= 1;
            //System.out.println("DP FileExists");
            if(!node.isTombstone(key)){
                if(node.tombstone(key)){
                    deleted = true;
                    //System.out.println("DP TombStoned!");
                }
            }
        }
        String deleted_str = "didn't delete (didn't have the pair active to begin with)";
        if(deleted){
            deleted_str = "deleted the pair";
        }
        if (nextRep < 0){
            nextRep += REPLICATION_FACTOR + 1;
            if(activeNodesSorted.size() > 1){
                for(String node : activeNodesSorted){
                    if(!node.equals(this.node.getNodeId())){
                        try {
                            message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". Sending to the closest who is not me: " + node;
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(node, this.node.getStorePort(), "D " + String.valueOf(nextRep) + " " + key);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            }
            else{
                message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". I'm alone in the cluster ;(. There aren't any active pairs";
            }
        }
        else if (nextRep > 0){

            boolean send = false;
            boolean sent = false;
            for(String node : activeNodesSorted){
                if(send){
                    try {
                        message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". Sending to the next node: " + node;
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(node, this.node.getStorePort(), "D " + String.valueOf(nextRep) + " " + key);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    sent = true;
                    break;
                }
                if(node.equals(this.node.getNodeId())){
                    send = true;
                }
            }
            if(!sent){
                if(activeNodesSorted.size() < REPLICATION_FACTOR){
                    message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". There aren't enough nodes in the cluster. There aren't any active pairs";
                }
                else{
                    try {
                        message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". I was the last of my list so sending it back to the closest: " + activeNodesSorted.get(0);
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(activeNodesSorted.get(0), node.getStorePort(), "D " + String.valueOf(nextRep) + " " + key);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        else{
            message = this.node.getNodeId() + " DELETE-> I " + deleted_str + ". All pairs already deleted";
        }
        if (messenger != null){
            messenger.run();
            writer.println(messenger.getAnswer());
            System.out.println("ANS:\n" + messenger.getAnswer() + "\n---");
        }
        else{
            writer.println(message + "\n" + node.getMSG_END_SERVICE());
            System.out.println("ANS:\n" + message + "\n" + node.getMSG_END_SERVICE() + "\n---");
        }
    }
}
