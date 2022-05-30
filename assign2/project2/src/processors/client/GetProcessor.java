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
            writer.println(value);
        }
        else{
            List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
            for(String node : activeNodesSorted){
                if(!node.equals(nodeId)){
                    try {
                        this.threadPool.execute(new MessageSender(node, port, "get " + key));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        this.threadPool.shutdown();
    }
}
