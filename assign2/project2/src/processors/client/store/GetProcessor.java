package processors.client.store;

import protocol.Node;
import utils.MessageSender;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetProcessor implements Runnable{
    private MessageSender messenger = null;
    private final String key;
    private final Node node;
    private final Socket socket;
    private final PrintWriter writer;
    private String message = "";
    private final int retryFactor;

    public GetProcessor(Node node, String key, int retryFactor, Socket socket) throws IOException {
        this.node = node;
        this.key = key;
        if(retryFactor < 0){
            this.retryFactor = node.getRETRY_FACTOR();
        }
        else{
            this.retryFactor = retryFactor;
        }
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        //System.out.println("GP Key: " + key);
        System.out.println(node.getNodeId() + " GP RetryFactor: " + retryFactor);
    }

    public void run(){
        if(Utils.fileExists(node.getHashedId() + File.separator +"storage" + File.separator + key + ".txt") && !Utils.getFileContent(node.getHashedId() + File.separator + "storage" + File.separator + key + ".txt").equals(Utils.MSG_TOMBSTONE)){
            String value = Utils.getFileContent(node.getHashedId() + File.separator +"storage" + File.separator + key + ".txt");
            message = this.node.getNodeId() + " GET-> Value Fetched: " + value;
            //System.out.println(message);
            //writer.println(message);
        }
        else{
            List<String> activeNodesSorted = Utils.getActiveMembersSorted(node.getHashedId(), key);
            for(int i = 0; i < activeNodesSorted.size(); i++){
                System.out.println("GP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
            }
            if(activeNodesSorted.get(0).equals(this.node.getNodeId())){
                if(retryFactor == 0 || activeNodesSorted.size() == 1){
                    message = this.node.getNodeId() + " GET-> Value not Found!";
                    //System.out.println(message);
                    //writer.println(message);
                }
                else{
                    int index = (int) Math.floor(Math.random()*(activeNodesSorted.size() - 1) + 1);
                    //System.out.println(node.getNodeId() + " GP Random Index: " + index);
                    //System.out.println(node.getNodeId() + " GP Random Node: " + activeNodesSorted.get(index));
                    try {
                        message = this.node.getNodeId() + " GET-> Retrying and sending request to the random node: " + activeNodesSorted.get(index);
                        //System.out.println(message);
                        //writer.println(message);
                        messenger = new MessageSender(activeNodesSorted.get(index), this.node.getStorePort(), "G " + String.valueOf(retryFactor - 1) + " " + key);
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else{
                try {
                    message = this.node.getNodeId() + " GET-> Sending to the closest node: " + activeNodesSorted.get(0);
                    //System.out.println(message);
                    //writer.println(message);
                    messenger = new MessageSender(activeNodesSorted.get(0), this.node.getStorePort(), "G " + String.valueOf(retryFactor) + " " + key);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
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
