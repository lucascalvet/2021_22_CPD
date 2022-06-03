package processors.client;

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
    private final int port;
    private MessageSender messenger = null;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final Socket socket;
    private final PrintWriter writer;
    private String message = "";

    public GetProcessor(String nodeId, String key, int port, Socket socket) throws IOException {
        this.port = port;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        //System.out.println("GP Key: " + key);
    }

    public void run(){
        if(Utils.fileExists(hashedId + File.separator +"storage" + File.separator + key + ".txt") && !Utils.getFileContent(hashedId + File.separator + "storage" + File.separator + key + ".txt").equals(Utils.MSG_TOMBSTONE)){
            String value = Utils.getFileContent(hashedId + File.separator +"storage" + File.separator + key + ".txt");
            message = nodeId + " GET-> Value Fetched: " + value;
            //System.out.println(message);
            //writer.println(message);
        }
        else{
            List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
            for(int i = 0; i < activeNodesSorted.size(); i++){
                System.out.println("GP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
            }
            if(activeNodesSorted.get(0).equals(nodeId)){
                message = nodeId + " GET-> Value not Found!";
                //System.out.println(message);
                //writer.println(message);
            }
            else{
                for(String node : activeNodesSorted){
                    if(!node.equals(nodeId)){
                        try {
                            message = nodeId + " GET-> Sending to the closest node: " + node;
                            //System.out.println(message);
                            writer.println(message);
                            messenger = new MessageSender(node, port, "G 1 " + key);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
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
