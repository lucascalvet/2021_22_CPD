package protocol;

import utils.Utils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final String hashedId;
    private final InetAddress nodeAddress;
    private final Integer storePort;
    private final int REPLICATION_FACTOR = 3;
    private final int RETRY_FACTOR = 3;
    private static final String MSG_TOMBSTONE = "TOMBSTONE";

    private boolean needsReconnect;
    private final ExecutorService threadPool;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) throws UnknownHostException {
        System.out.println("Creating filesystem directory: " + new File(Utils.BASE_DIR).mkdirs());
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.nodeAddress = InetAddress.getByName(nodeId);
        this.storePort = storePort;
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.needsReconnect = false;
        this.threadPool = Executors.newFixedThreadPool(threadCount);
        System.out.println(Thread.currentThread().getName() + ": Created thread pool with " + threadCount + " threads");
        this.createDirectories();
        this.initCounter();
    }

    public int getREPLICATION_FACTOR() {
        return REPLICATION_FACTOR;
    }

    public int getRETRY_FACTOR() {
        return RETRY_FACTOR;
    }

    public String getMSG_TOMBSTONE() {
        return MSG_TOMBSTONE;
    }

    public String getMSG_END_SERVICE() {
        return Utils.MSG_END_SERVICE;
    }

    public String getMSG_END() {
        return Utils.MSG_END;
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

    public File getStorageDir() {
        return new File(Utils.BASE_DIR + getHashedId() + File.separator + "storage");
    }

    private String getFileRelativePath(String key, boolean tombstoned) {
        if (tombstoned) {
            key = getMSG_TOMBSTONE() + key;
        }
        return getHashedId() + File.separator + "storage" + File.separator + key + ".txt";
    }

    public boolean getNeedsReconnect() {
        return needsReconnect;
    }

    public synchronized void setNeedsReconnect(boolean needsReconnect) {
        this.needsReconnect = needsReconnect;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void createDirectories() {
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
        } else {
            Utils.updateLogs(this.nodeId, -1, this.hashedId);
        }
    }

    public synchronized void initCounter() {
        Map<String, Integer> logs = Utils.readLogs(this.getHashedId());
        if (!logs.containsKey(this.nodeId)) {
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

    public synchronized String get32OrMoreLogs() {
        return Utils.getNOrMoreLogLines(this.hashedId, 32);
    }

    public synchronized void setAllLogs(Map<String, Integer> logs) {
        logs.put(this.nodeId, this.getCounter());
        Utils.setAllLogs(logs, this.hashedId);
    }

    public synchronized List<Boolean> updateAllLogs(Map<String, Integer> logs) {
        return Utils.updateAllLogs(logs, this.hashedId);
    }

    public synchronized boolean storePair(String value) {
        String key = Utils.encodeToHex(value);
        if (isTombstone(Utils.encodeToHex(value))) {
            Utils.deleteFile(getFileRelativePath(key, true));
        }
        return Utils.writeToFile(getFileRelativePath(key, false), value);
    }

    public synchronized boolean tombstone(String key) {
        //return Utils.writeToFile(getHashedId() + File.separator + "storage" + File.separator + key + ".txt", getMSG_TOMBSTONE());
        boolean renamed = Utils.renameFile(getFileRelativePath(key, false), getFileRelativePath(key, true));
        if (renamed) {
            return Utils.writeToFile(getFileRelativePath(key, true), "");
        }
        return false;
    }

    public synchronized boolean pairExists(String key) {
        return Utils.fileExists(getFileRelativePath(key, false)) || Utils.fileExists(getFileRelativePath(key, false));
    }

    public synchronized boolean isTombstone(String key) {
        //return Utils.getFileContent(getHashedId() + File.separator + "storage" + File.separator + key + ".txt").equals(getMSG_TOMBSTONE());
        return Utils.fileExists(getFileRelativePath(key, true));
    }

    public synchronized boolean isAvailable(String key) {
        return pairExists(key) && !isTombstone(key);
    }

    public int compareDistances(String key, String otherId) {
        return Utils.hashDistance(key, Utils.encodeToHex(otherId)).compareTo(Utils.hashDistance(key, hashedId));
    }

    public String getKey(String value) {
        return Utils.encodeToHex(value);
    }

    public synchronized String getValue(String key) {
        return Utils.getFileContent(getFileRelativePath(key, false));
    }

    public synchronized List<String> getActiveMembers() {
        return Utils.getActiveMembers(getHashedId());
    }

    public synchronized List<String> getActiveMembersSorted(String hashedTarget) {
        return Utils.getActiveMembersSorted(getHashedId(), hashedTarget);
    }

    public void run() {
        StorageProtocol storageProtocol = new StorageProtocol(this);
        Thread storeThread = new Thread(storageProtocol);
        storeThread.start();
    }
}
