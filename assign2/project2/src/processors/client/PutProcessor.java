package processors.client;

import utils.MessageSender;
import utils.Utils;

import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PutProcessor implements Runnable{
    private final String value;
    private int replicationFactor;
    private final int port;
    private final String key;
    private final String nodeId;
    private final String hashedId;
    private final PrintWriter writer;
    private final boolean store;
    private final int REPLICATION_FACTOR = 3;
    private final int NTHREADS = 2;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    public PutProcessor(String nodeId, String opArg, int replicationFactor, int port, PrintWriter writer){
        this.port = port;
        this.replicationFactor = replicationFactor;
        this.value = opArg;
        this.nodeId = nodeId;
        this.key = Utils.encodeToHex(value);
        this.hashedId = Utils.encodeToHex(nodeId);
        this.store = !Utils.fileExists(hashedId + "\\storage\\" + key + ".txt");
        this.writer = writer;

        //System.out.println("PP RepFactor: " + replicationFactor);
        //System.out.println("PP opArg: " + opArg);
        //System.out.println("PP nodeId: " + nodeId);
        //System.out.println("PP Value: " + value);
        //System.out.println("PP Key: " + key);
    }

    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
        for(int i = 0; i < activeNodesSorted.size(); i++){
            System.out.println("PP AN" + Integer.toString(i) + "->" + activeNodesSorted.get(i));
        }
        if(replicationFactor == -1){
            if(activeNodesSorted.get(0).equals(nodeId)){
                if(store) Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, true);
                while(activeNodesSorted.size() < 2){
                    activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
                }
                for(String node : activeNodesSorted){
                    if(!node.equals(nodeId)){
                        int nextRep = REPLICATION_FACTOR;
                        if(store){
                            nextRep -= 1;
                        }
                        try {
                            System.out.println("PP 1");
                            writer.println("PP I was the closest. Sending to the next one");
                            this.threadPool.execute(new MessageSender(activeNodesSorted.get(1), port, "P " + String.valueOf(nextRep) + " " + value));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            }
            else{
                try {
                    System.out.println("PP 2");
                    writer.println("PP I wasn't the closest. Sending to the closest");
                    this.threadPool.execute(new MessageSender(activeNodesSorted.get(0), port, "P " + String.valueOf(REPLICATION_FACTOR) + " " + value));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if(replicationFactor > 0){
            int nextRep = replicationFactor;
            if(store){
                Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, true);
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
                            System.out.println("PP 3");
                            writer.println("PP Sending to the next one");
                            this.threadPool.execute(new MessageSender(node, port, "P " + String.valueOf(nextRep) + " " + value));
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
                        System.out.println("PP 4");
                        writer.println("PP Sending to the closest");
                        this.threadPool.execute(new MessageSender(activeNodesSorted.get(0), port, "P " + String.valueOf(nextRep) + " " + value));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        this.threadPool.shutdown();
    }
}
