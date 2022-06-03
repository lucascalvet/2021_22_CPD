package processors.client.membership;

import processors.node.LeaveTransferProcessor;
import protocol.Node;
import protocol.MembershipNode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.Socket;

public class LeaveProcessor implements Runnable {
    private final Node node;
    private final Socket clientSocket;
    private final MembershipNode membershipNode;
    private final ExecutorService threadPool;
    private String result = "";

    public LeaveProcessor(Node node, MembershipNode membershipNode, Socket clientSocket) {
        this.node = node;
        this.clientSocket = clientSocket;
        this.membershipNode = membershipNode;
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.threadPool = Executors.newFixedThreadPool(threadCount);
    }

    public LeaveProcessor(Node node, MembershipNode membershipNode) {
        this.node = node;
        this.clientSocket = null;
        this.membershipNode = membershipNode;
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.threadPool = Executors.newFixedThreadPool(threadCount);
    }

    public String getResult() {
        return result;
    }

    @Override
    public void run() {
        PrintWriter clientWriter = null;
        if (clientSocket != null) {
            try {
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (node.getCounter() % 2 != 0) {
            if (clientWriter != null) {
                clientWriter.println(node.getNodeId() + " LEAVE-> Node isn't joined to any cluster! Aborting.");
                clientWriter.println(node.getMSG_END_SERVICE());
            }
            result = node.getNodeId() + " LEAVE-> Node isn't joined to any cluster! Aborting.";
            return;
        }

        //Storage change before membership message is transmitted
        File dir = node.getStorageDir();
        File[] storageListing = dir.listFiles();
        if (storageListing != null) {
            for (File child : storageListing) {
                String key = child.getName().replaceFirst("[.][^.]+$", "");
                if(!key.contains(node.getMSG_TOMBSTONE())){
                    var activeMembersSorted = node.getActiveMembersSorted(key);
                    if(activeMembersSorted.size() > 0){
                        threadPool.execute(new LeaveTransferProcessor(node, key));
                    }
                    else{
                        node.tombstone(key);
                    }
                }
            }
        }

        System.out.println("Quitting multicast thread.");

        this.membershipNode.stop();

        String info = "Initializing leave process.";
        System.out.println(info);
        if (clientWriter != null)
            clientWriter.println(node.getNodeId() + " LEAVE-> " + info);

        this.node.setCounter();

        // creating >> L << message
        String lMessage = "L " + node.getNodeId() + " " + node.getCounter();

        // multicasting message
        DatagramSocket socket;
        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = lMessage.getBytes();

            DatagramPacket packet;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(node.getMulticastAddr().getHostName()), node.getMulticastPort());

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            result = node.getNodeId() + " LEAVE-> Error leaving cluster.";
            throw new RuntimeException(e);
        }

        info = "Finished leave process.";
        System.out.println(info);
        if (clientWriter != null) {
            clientWriter.println(node.getNodeId() + " LEAVE-> " + info);
            clientWriter.println(node.getMSG_END_SERVICE());
        }
        result = node.getNodeId() + " LEAVE-> " + info;
    }
}
