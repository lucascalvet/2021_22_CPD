import jdk.jshell.execution.Util;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {

    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final String hashedId;
    private final Integer storePort;
    private MembershipProtocol membershipProtocol;
    private StoreOperations storeOperations;
    private final int NTHREADS = 2;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) {
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.storePort = storePort;
        this.membershipProtocol = new MembershipProtocol(multicastAddr.getHostName(), multicastPort);
        this.storeOperations = new StoreOperations(nodeId, storePort);
        createDirectories();
    }

    public void createDirectories(){
        Utils.makeDir(hashedId);
        Utils.writeToFile(hashedId + "\\membership_log.txt", nodeId + " 0\n", true);
        Utils.makeDir(hashedId + "\\storage");
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        // create socket to membership
        this.threadPool.execute(this.membershipProtocol);
        // create socket to store
        this.threadPool.execute(this.storeOperations);

        this.threadPool.shutdown();
    }
}
