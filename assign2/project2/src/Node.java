import jdk.jshell.execution.Util;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Node {

    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final String hashedId;
    private final Integer storePort;
    private MembershipProtocol membershipProtocol;
    private StoreOperations storeOperations;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) {
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.storePort = storePort;
        this.membershipProtocol = new MembershipProtocol(nodeId, multicastPort);
        this.storeOperations = new StoreOperations(nodeId, storePort);
        createDirectories();
    }

    public void createDirectories(){
        Utils.makeDir(hashedId);
        Utils.writeToFile(hashedId + "\\membership_log.txt", hashedId + "\n", true);
        Utils.makeDir(hashedId + "\\storage");
    }

    public void run() {
        // create socket to membership
        membershipProtocol.run();
        // create socket to store
        storeOperations.run();
    }
}
