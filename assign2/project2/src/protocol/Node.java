package protocol;

import utils.Utils;

import java.net.*;

public class Node {
    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final String hashedId;
    private final InetAddress nodeAddress;
    private final Integer storePort;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) throws UnknownHostException {
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.nodeAddress = InetAddress.getByName(nodeId);
        this.storePort = storePort;
        createDirectories();
    }

    public InetAddress getMulticastAddr() {
        return multicastAddr;
    }

    public Integer getMulticastPort() {
        return multicastPort;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHashedId() {
        return hashedId;
    }

    public InetAddress getAddress() throws UnknownHostException {
        return nodeAddress;
    }

    public Integer getStorePort() {
        return storePort;
    }

    public void createDirectories(){
        Utils.makeDir(hashedId);
        //Utils.writeToFile(hashedId + "\\membership_log.txt", nodeId + " 0\n", true);
        Utils.writeToFile(hashedId + "\\membership_log.txt", "127.0.0.1 0\n127.0.0.2 0\n127.0.0.3 0\n127.0.0.6 0\n127.0.0.5 0\n", true);
        Utils.makeDir(hashedId + "\\storage");
    }

    public synchronized void addLog(String nodeId, int counter) {
        Utils.updateLogs(nodeId, counter, this.hashedId);
    }

    public synchronized String get32Logs() {
        return Utils.getNLogLines(this.hashedId, 32);
    }

    public void run() throws UnknownHostException {
        //Thread multicastThread = new Thread(new MembershipNode(this), "Multicast Thread");
        Thread storeThread = new Thread(new StorageProtocol(nodeId, storePort), "Store Thread");

        //multicastThread.start();
        storeThread.start();
    }
}
