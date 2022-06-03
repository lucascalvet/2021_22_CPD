package processors.client.store;

import utils.MessageSender;
import utils.Utils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeleteProcessor implements Runnable{
    private int replicationFactor;
    private final int port;
    private MessageSender messenger = null;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final Socket socket;
    private final PrintWriter writer;
    private final int REPLICATION_FACTOR = 3;
    private String message = "";

    public DeleteProcessor(String nodeId, String key, int replicationFactor, int port, Socket socket) throws IOException {
        this.port = port;
        this.replicationFactor = replicationFactor;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);


        //System.out.println("DP Key: " + key);
        //System.out.println("DP Replication Factor: " + replicationFactor);
    }

    public void run(){
        boolean deleted = false;
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
        int nextRep = replicationFactor;
        if(Utils.fileExists(hashedId + File.separator + "storage" + File.separator + key + ".txt")){
            nextRep -= 1;
            //System.out.println("DP FileExists");
            if(!Utils.getFileContent(hashedId + File.separator + "storage" + File.separator + key + ".txt").equals(Utils.MSG_TOMBSTONE)){
                if(Utils.writeToFile(hashedId + File.separator + "storage" + File.separator + key + ".txt", Utils.MSG_TOMBSTONE, false)){
                    deleted = true;
                    //System.out.println("DP TombStoned!");
                    //writer.println("Pair successfully deleted!");
                }
                /*
                if(Utils.deleteFile(hashedId + File.separator + "storage" + File.separator + key + ".txt")){
                    writer.println("Pair successfully deleted!");
                }
                */
            }
        }
        String deleted_str = "didn't delete (didn't have the pair active to begin with)";
        if(deleted){
            deleted_str = "deleted the pair";
        }
        if (nextRep < 0){
            nextRep += REPLICATION_FACTOR + 1;
            for(String node : activeNodesSorted){
                if(!node.equals(nodeId)){
                    try {
                        message = nodeId + " DELETE-> I " + deleted_str + ". Sending to the closest who is not me: " + node;
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(node, port, "D " + String.valueOf(nextRep) + " " + key);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }
        else if (nextRep > 0){
            boolean send = false;
            boolean sent = false;
            for(String node : activeNodesSorted){
                if(send){
                    try {
                        message = nodeId + " DELETE-> I " + deleted_str + ". Sending to the next node: " + node;
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(node, port, "D " + String.valueOf(nextRep) + " " + key);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                    sent = true;
                    break;
                }
                if(node.equals(nodeId)){
                    send = true;
                }
            }
            if(!sent){
                try {
                    message = nodeId + " DELETE-> I " + deleted_str + ". I was the last of my list so sending it back to the closest: " + activeNodesSorted.get(0);
                    //System.out.println(message);
                    writer.println(message);
                    messenger = new MessageSender(activeNodesSorted.get(0), port, "D " + String.valueOf(nextRep) + " " + key);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else{
            message = nodeId + " DELETE-> I " + deleted_str + ". All pairs already deleted";
        }
        if (messenger != null){
            messenger.run();
            writer.println(messenger.getAnswer());
            System.out.println("ANS:\n" + messenger.getAnswer() + "\n---");
        }
        else{
            writer.println(message + "\n" + Utils.MSG_END_SERVICE);
            System.out.println("ANS:\n" + message + "\n" + Utils.MSG_END_SERVICE + "\n---");
        }
    }
}
