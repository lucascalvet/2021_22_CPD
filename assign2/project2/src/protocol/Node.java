package protocol;

import utils.Utils;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
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
        System.out.println("Creating filesystem directory: " + new File(Utils.BASE_DIR).mkdirs());
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.nodeAddress = InetAddress.getByName(nodeId);
        this.storePort = storePort;
        this.createDirectories();
        this.setCounter();
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
        System.out.println("Node hash: " + this.hashedId);
        //Utils.writeToFile(hashedId + File.separator + "membership_log.txt", "127.0.0.1 0\n127.0.0.2 0\n127.0.0.3 0\n127.0.0.4 0\n127.0.0.5 0\n", true);
        Utils.makeDir(hashedId + File.separator + "storage");
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
        System.out.println("Adding " + nodeId + " " + counter + " to the membership log.");
        Utils.updateLogs(nodeId, counter, this.hashedId);
    }

    public synchronized String get32Logs() {
        return Utils.getNLogLines(this.hashedId, 32);
    }

    public synchronized void setAllLogs(Map<String, Integer> logs) {
        logs.put(this.nodeId, this.getCounter());
        Utils.setAllLogs(logs, this.hashedId);
    }

    public synchronized void updateAllLogs(Map<String, Integer> logs) {
        Utils.updateAllLogs(logs, this.hashedId);
    }

    public void run() throws UnknownHostException, RemoteException, AlreadyBoundException, MalformedURLException {
        System.setProperty("java.rmi.server.hostname","192.168.56.1");
        StorageProtocol storageProtocol = new StorageProtocol(this);
        // rmi
        Naming.rebind("Membership", storageProtocol);

        Thread storeThread = new Thread(storageProtocol);
        storeThread.start();
    }
}
