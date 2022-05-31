package processors.client;

import utils.MessageSender;
import utils.Utils;

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeleteProcessor implements Runnable{
    private int replicationFactor;
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final int REPLICATION_FACTOR = 3;
    private final int NTHREADS = 2;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    public DeleteProcessor(String nodeId, String key, int replicationFactor, int port, PrintWriter writer){
        this.port = port;
        this.replicationFactor = replicationFactor;
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
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
        int nextRep = replicationFactor;
        if(Utils.fileExists(hashedId + "\\storage\\" + key + ".txt")){
            nextRep -= 1;
            if(Utils.deleteFile(hashedId + "\\storage\\" + key + ".txt")){
                writer.println("Pair successfully deleted!");
            }
        }
        if (nextRep < 0){
            nextRep += REPLICATION_FACTOR + 1;
            for(String node : activeNodesSorted){
                if(!node.equals(nodeId)){
                    try {
                        this.threadPool.execute(new MessageSender(node, port, "D|" + key + "|" + String.valueOf(nextRep)));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        if(nextRep > 0){
            boolean send = false;
            boolean sent = false;
            for(String node : activeNodesSorted){
                if(send){
                    try {
                        this.threadPool.execute(new MessageSender(node, port, "D|" + key + "|" + String.valueOf(nextRep)));
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
                    this.threadPool.execute(new MessageSender(activeNodesSorted.get(0), port, "D|" + key + "|" + String.valueOf(nextRep)));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.threadPool.shutdown();
    }
}
