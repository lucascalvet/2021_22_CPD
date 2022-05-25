import jdk.jshell.execution.Util;

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

    PutProcessor(String nodeId, String opArg, int replicationFactor, int port, PrintWriter writer){
        this.port = port;
        this.replicationFactor = replicationFactor;
        if(replicationFactor == -1){
            this.value = Utils.getFileContent(opArg);
            System.out.println("File: " + opArg);
        }
        else{
            this.value = opArg;
        }
        this.nodeId = nodeId;
        this.key = Utils.encodeToHex(value);
        this.hashedId = Utils.encodeToHex(nodeId);
        this.store = !Utils.fileExists(hashedId + "\\storage\\" + key + ".txt");
        this.writer = writer;

        System.out.println("Value: " + value);
        System.out.println("Key: " + key);
    }


    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        List<String> activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
        if(replicationFactor == -1){
            if(activeNodesSorted.get(0).equals(nodeId)){
                if(store) Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, true);
                writer.println("Pair successfully stored!");
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
                            this.threadPool.execute(new MessageSender(activeNodesSorted.get(1), port, "put " + value + " " + String.valueOf(nextRep)));
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    }
                }
            }
            else{
                try {
                    this.threadPool.execute(new MessageSender(activeNodesSorted.get(0), port, "put " + value + " " + String.valueOf(REPLICATION_FACTOR)));
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if(replicationFactor > 0){
            int nextRep = replicationFactor;
            if(store){
                Utils.writeToFile(hashedId + "\\storage\\" + key + ".txt", value, true);
                nextRep -= 1;
            }
            if(nextRep > 0){
                while(activeNodesSorted.size() < 4 - nextRep){
                    activeNodesSorted = Utils.getActiveMembersSorted(hashedId, key);
                }
                boolean send = false;
                boolean sent = false;
                for(String node : activeNodesSorted){
                    if(send){
                        try {
                            this.threadPool.execute(new MessageSender(node, port, "put " + value + " " + String.valueOf(nextRep)));
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
                        this.threadPool.execute(new MessageSender(activeNodesSorted.get(0), port, "put " + value + " " + String.valueOf(nextRep)));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        this.threadPool.shutdown();
    }
}
