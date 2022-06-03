package processors.node;

import protocol.Node;
import utils.MessageSender;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
        if(!node.getNodeId().equals(nodeId) && !node.getActiveMembers().contains(nodeId)){
            File dir = node.getStorageDir();
            File[] storageListing = dir.listFiles();
            if (storageListing != null) {
                for (File child : storageListing) {
                    boolean send = false;
                    boolean delete = false;
                    String key = child.getName().replaceFirst("[.][^.]+$", "");
                    if(!key.contains(node.getMSG_TOMBSTONE())){
                        String value = node.getValue(key);
                        var activeMembersSorted = node.getActiveMembersSorted(key);
                        if(activeMembersSorted.size() > 0 && node.getActiveMembers().size() < node.getREPLICATION_FACTOR() && activeMembersSorted.get(0).equals(node.getNodeId())){
                            send = true;
                        }
                        else{
                            if(activeMembersSorted.size() >= node.getREPLICATION_FACTOR() && activeMembersSorted.get(node.getREPLICATION_FACTOR() - 1).equals(node.getNodeId()) && node.compareDistances(key, nodeId) == -1){
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
                            node.tombstone(key);
                        }
                    }
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
