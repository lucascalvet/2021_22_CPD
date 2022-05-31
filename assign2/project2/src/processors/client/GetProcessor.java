package processors.client;

import utils.MessageSender;
import utils.Utils;

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetProcessor implements Runnable{
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final int NTHREADS = 2;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    public GetProcessor(String nodeId, String key, int port, PrintWriter writer){
        this.port = port;
        this.nodeId = nodeId;
        this.key = key;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.writer = writer;

        System.out.println("Key: " + key);
    }

    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        if(Utils.fileExists(hashedId + "\\storage\\" + key + ".txt")){
            String value = Utils.getFileContent(hashedId + "\\storage\\" + key + ".txt");
            System.out.println("GP Value Fetched: " + value);
            writer.println(value);
        }
        else{
            List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
            for(int i = 0; i < activeNodesSorted.size(); i++){
                System.out.println("GP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
            }
            if(activeNodesSorted.get(0).equals(nodeId)){
                System.out.println("GP Value Not Found");
                writer.println("Value Not Found");
            }
            else{
                for(String node : activeNodesSorted){
                    if(!node.equals(nodeId)){
                        try {
                            System.out.println("GP ASKING " + node);
                            this.threadPool.execute(new MessageSender(node, port, "G|" + key));
                            break;
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        this.threadPool.shutdown();
    }
}
