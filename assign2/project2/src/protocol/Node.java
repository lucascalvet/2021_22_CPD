package protocol;

import utils.Utils;

import java.io.File;
import java.net.*;
import java.util.Map;

public class Node {
    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final String hashedId;
    private final InetAddress nodeAddress;
    private final Integer storePort;
    private final int REPLICATION_FACTOR = 3;
    private final int RETRY_FACTOR = 3;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) throws UnknownHostException {
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.nodeAddress = InetAddress.getByName(nodeId);
        this.storePort = storePort;
        createDirectories();
        //setCounter();
    }

    public int getREPLICATION_FACTOR(){return REPLICATION_FACTOR;}

    public int getRETRY_FACTOR(){return RETRY_FACTOR;}

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
        Utils.writeToFile(hashedId + File.separator + "membership_log.txt", "127.0.0.1 0\n127.0.0.2 0\n127.0.0.3 0\n127.0.0.4 0\n127.0.0.5 0\n", true);
        //Utils.writeToFile(hashedId + File.separator + "membership_log.txt", "127.0.0.1 0\n", true);
        //Utils.writeToFile(hashedId + File.separator + "membership_log.txt", "127.0.0.1 0\n127.0.0.2 0\n", true);
        Utils.makeDir(hashedId + "\\storage");
    }

    public synchronized int getCounter() {
        Map<String, Integer> logs = Utils.readLogs(this.getHashedId());
        if (logs.containsKey(this.nodeId)) {
            return logs.get(this.nodeId);
        }
        return -1;
    }

    public synchronized void setCounter() {
        Map<String, Integer> logs = Utils.readLogs(this.getHashedId());
        if (logs.containsKey(this.nodeId)) {
            Utils.updateLogs(this.nodeId, logs.get(this.nodeId) + 1, this.hashedId);
        }
        else {
            Utils.updateLogs(this.nodeId, -1, this.hashedId);
        }
    }

    public synchronized void addLog(String nodeId, int counter) {
        Utils.updateLogs(nodeId, counter, this.hashedId);
    }

    public synchronized String get32Logs() {
        return Utils.getNLogLines(this.hashedId, 32);
    }

    public synchronized void setAllLogs(Map<String, Integer> logs) {
        Utils.updateAllLogs(logs, this.hashedId);
    }

    public void run() throws UnknownHostException {
        Thread storeThread = new Thread(new StorageProtocol(this), "Store Thread");
        storeThread.start();
    }
}