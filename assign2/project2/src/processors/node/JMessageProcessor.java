package processors.node;

import protocol.Node;
import utils.MessageSender;
import utils.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JMessageProcessor implements Runnable{
    private final int port;
    private final String nodeId;
    private final int counter;
    private final Node node;
    private final ExecutorService threadPool;

    public JMessageProcessor(Node node, String nodeId, int port, int counter) {
        this.port = port;
        this.nodeId = nodeId;
        this.counter = counter;
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
    }

    @Override
    public void run() {
        //Storage change before membership message is transmitted
        File dir = new File(Utils.BASE_DIR + node.getHashedId() + File.separator + "storage");
        File[] storageListing = dir.listFiles();
        if (storageListing != null) {
            for (File child : storageListing) {
                boolean send = false;
                boolean delete = false;
                String value = Utils.getFileContent(node.getHashedId() + File.separator + "storage" + File.separator + child.getName());
                String key = child.getName().replaceFirst("[.][^.]+$", "");
                if(Utils.getActiveMembers(node.getHashedId()).size() < node.getREPLICATION_FACTOR()){
                    send = true;
                }
                else{
                    if(Utils.getActiveMembersSorted(node.getHashedId(), key).get(2).equals(node.getNodeId()) && Utils.hashDistance(key, Utils.encodeToHex(nodeId)).compareTo(Utils.hashDistance(key, node.getHashedId())) == -1){
                        send = true;
                        delete = true;
                    }
                }
                if(send){
                    try {
                        threadPool.execute(new MessageSender(nodeId, node.getStorePort(), "P 1 " + value));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(delete){
                    Utils.writeToFile(node.getHashedId() + File.separator + "storage" + File.separator + child.getName(), Utils.MSG_TOMBSTONE);
                }
            }
        }

        node.addLog(nodeId, counter);

        // wait for a random time from 0 to 1 secs
        try {
            Thread.sleep((long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // send the 32 most recent logs
        try (Socket socket = new Socket(InetAddress.getByName(nodeId), port)) {
            String toSend = node.get32OrMoreLogs();
            System.out.println("Sending last 32 logs to " + socket.getInetAddress().getHostName());
            String message = "M " + node.getNodeId() + "\n" + toSend + "end";

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
