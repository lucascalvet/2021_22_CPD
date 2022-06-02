package processors.client;

import utils.MessageSender;
import utils.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PutProcessor implements Runnable{
    private final String value;
    private int replicationFactor;
    private final int port;
    private MessageSender messenger = null;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final Socket socket;
    private final PrintWriter writer;
    private final boolean exists;
    private final boolean store;
    private final int REPLICATION_FACTOR = 3;
    private String message = "";

    public PutProcessor(String nodeId, String opArg, int replicationFactor, int port, Socket socket) throws IOException {
        this.port = port;
        this.replicationFactor = replicationFactor;
        this.value = opArg;
        this.nodeId = nodeId;
        this.key = Utils.encodeToHex(value);
        this.hashedId = Utils.encodeToHex(nodeId);
        this.exists = Utils.fileExists(hashedId + "\\storage\\" + key + ".txt");
        this.store = !exists || Utils.getFileContent(hashedId + "\\storage\\" + key + ".txt").equals(Utils.MSG_TOMBSTONE);
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        //System.out.println("PP RepFactor: " + replicationFactor);
        //System.out.println("PP opArg: " + opArg);
        //System.out.println("PP nodeId: " + nodeId);
        //System.out.println("PP Value: " + value);
        //System.out.println("PP Key: " + key);
    }

    public void run(){
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
        for(int i = 0; i < activeNodesSorted.size(); i++){
            System.out.println("PP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
        }
        String store_str = "didn't store (already had the pair stored)";
        if(replicationFactor == -1){
            if(activeNodesSorted.get(0).equals(nodeId)){
                if(store){
                    //System.out.println("PP Stored");
                    Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, !exists);
                }
                while(activeNodesSorted.size() < 2){
                    activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
                }
                for(String node : activeNodesSorted){
                    if(!node.equals(nodeId)){
                        int nextRep = REPLICATION_FACTOR;
                        if(store){
                            store_str = "stored the pair";
                            nextRep -= 1;
                        }
                        try {
                            message = nodeId + " PUT-> I was the closest and " + store_str + ". Sending to the next one: " + activeNodesSorted.get(1);
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(activeNodesSorted.get(1), port, "P " + String.valueOf(nextRep) + " " + value);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            }
            else{
                try {
                    message = nodeId + " PUT-> I wasn't the closest, so I didn't store. Sending to the closest node: " + activeNodesSorted.get(0);
                    //System.out.println(message);
                    writer.println(message);
                    messenger = new MessageSender(activeNodesSorted.get(0), port, "P " + String.valueOf(REPLICATION_FACTOR) + " " + value);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if(replicationFactor > 0){
            int nextRep = replicationFactor;
            if(store){
                //System.out.println("PP Stored");
                store_str = "stored the pair";
                Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, !exists);
            }
            nextRep -= 1;
            if(nextRep > 0){
                while(activeNodesSorted.size() < 4 - nextRep){
                    activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
                }
                boolean send = false;
                boolean sent = false;
                for(String node : activeNodesSorted){
                    if(send){
                        try {
                            message = nodeId + " PUT-> I " + store_str + ". Sending to the next one: " + node;
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(node, port, "P " + String.valueOf(nextRep) + " " + value);
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
                        message = nodeId + " PUT-> I " + store_str + ". I was the last of my list so sending it back to the closest: " + activeNodesSorted.get(0);
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(activeNodesSorted.get(0), port, "P " + String.valueOf(nextRep) + " " + value);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else{
                if(store){
                    message = nodeId + " PUT-> I stored the pair and all pairs are now stored";
                }
                else{
                    message = nodeId + " PUT-> I didn't store the pair but all pairs are now stored";
                }
            }
        }
        if (messenger != null){
            messenger.run();
            writer.println(messenger.getAnswer());
        }
        else{
            writer.println(message + "\n" + Utils.MSG_END_SERVICE);
        }
    }
}
