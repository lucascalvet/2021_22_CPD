package processors.node;

import protocol.Node;
import utils.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class JMessageProcessor implements Runnable{
    private final int port;
    private final String nodeId;
    private final int counter;
    private final Node node;

    public JMessageProcessor(Node node, String nodeId, int port, int counter) {
        this.port = port;
        this.nodeId = nodeId;
        this.counter = counter;
        this.node = node;
    }

    @Override
    public void run() {
        System.out.println("Adding " + nodeId + " " + counter + " to the membership log.");
        node.addLog(nodeId, counter);

        // wait for a random time from 0 to 3 secs
        try {
            Thread.sleep((long) (Math.random() * 1000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // send the 32 most recent logs
        try (Socket socket = new Socket(InetAddress.getByName(nodeId), port)) {
            String toSend = node.get32Logs();
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
