package processors.client.store;

import protocol.Node;
import utils.MessageSender;
import utils.Utils;

import java.io.File;
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
    private final Node node;
    private int replicationFactor;
    private MessageSender messenger = null;
    private final String key;
    private final Socket socket;
    private final PrintWriter writer;
    private final boolean exists;
    private final boolean store;
    private String message = "";

    public PutProcessor(Node node, String opArg, int replicationFactor, Socket socket) throws IOException {
        this.node = node;
        this.replicationFactor = replicationFactor;
        this.value = opArg;
        this.key = Utils.encodeToHex(value);
        this.exists = Utils.fileExists(node.getHashedId() + File.separator +"storage" + File.separator + key + ".txt");
        this.store = !exists || Utils.getFileContent(node.getHashedId() + File.separator + "storage" + File.separator + key + ".txt").equals(Utils.MSG_TOMBSTONE);
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        //System.out.println("PP RepFactor: " + replicationFactor);
        //System.out.println("PP opArg: " + opArg);
        //System.out.println("PP nodeId: " + nodeId);
        //System.out.println("PP Value: " + value);
        //System.out.println("PP Key: " + key);
    }

    public void run(){
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(node.getHashedId(), key);
        for(int i = 0; i < activeNodesSorted.size(); i++){
            System.out.println("PP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
        }
        String store_str = "didn't store (already had the pair stored)";
        if(replicationFactor == -1){
            if(activeNodesSorted.get(0).equals(node.getNodeId())){
                if(store){
                    store_str = "stored the pair";
                    //System.out.println("PP Stored");
                    Utils.writeToFile(node.getHashedId() + File.separator +"storage" + File.separator + key + ".txt", value, !exists);
                }
                if(activeNodesSorted.size() > 1){
                    int nextRep = node.getREPLICATION_FACTOR() - 1;
                    try {
                        message = node.getNodeId() + " PUT-> I was the closest and " + store_str + ". Sending to the next one: " + activeNodesSorted.get(1);
                        //System.out.println(message);
                        writer.println(message);
                        messenger = new MessageSender(activeNodesSorted.get(1), node.getStorePort(), "P " + String.valueOf(nextRep) + " " + value);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    message = node.getNodeId() + " PUT-> I " + store_str + ". I'm alone in the cluster ;( so we are missing " + String.valueOf(node.getREPLICATION_FACTOR() - 1) + " pair copies to be added when more nodes enter";
                }
            }
            else{
                try {
                    message = node.getNodeId() + " PUT-> I wasn't the closest, so I didn't store. Sending to the closest node: " + activeNodesSorted.get(0);
                    //System.out.println(message);
                    writer.println(message);
                    messenger = new MessageSender(activeNodesSorted.get(0), node.getStorePort(), "P " + String.valueOf(node.getREPLICATION_FACTOR()) + " " + value);
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
                Utils.writeToFile(node.getHashedId() + File.separator +"storage" + File.separator + key + ".txt", value, !exists);
            }
            nextRep -= 1;
            if(nextRep > 0){
                boolean send = false;
                boolean sent = false;
                for(String node : activeNodesSorted){
                    if(send){
                        try {
                            message = this.node.getNodeId() + " PUT-> I " + store_str + ". Sending to the next one: " + node;
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(node, this.node.getStorePort(), "P " + String.valueOf(nextRep) + " " + value);
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
                    if(activeNodesSorted.size() < node.getREPLICATION_FACTOR()){
                        message = node.getNodeId() + " PUT-> I " + store_str + ". But there aren't enough nodes in the cluster, so we are missing " + String.valueOf(nextRep) + " pair copies to be added when more nodes enter";
                    }
                    else{
                        try {
                            message = node.getNodeId() + " PUT-> I " + store_str + ". I was the last of my list so sending it back to the closest: " + activeNodesSorted.get(0);
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(activeNodesSorted.get(0), node.getStorePort(), "P " + String.valueOf(nextRep) + " " + value);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            else{
                if(store){
                    message = node.getNodeId() + " PUT-> I stored the pair and all pairs are now stored";
                }
                else{
                    message = node.getNodeId() + " PUT-> I didn't store the pair but all pairs are now stored";
                }
            }
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
