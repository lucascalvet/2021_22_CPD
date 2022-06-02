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
    private final String hashedId;
    private final int counter;
    private final Node node;

    public JMessageProcessor(Node node, String nodeId, int port, int counter) {
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.counter = counter;
        this.node = node;
    }

    @Override
    public void run() {
        Utils.updateLogs(nodeId, counter, hashedId);

        // TODO: missing waiting for random time and choosing the node -> to replace in "true"

        if (true) {
            try (Socket socket = new Socket(InetAddress.getByName(nodeId), port)) {

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                String toSend = node.get32Logs();
                writer.print(toSend);

            } catch (UnknownHostException ex) {

                System.out.println("Server not found: " + ex.getMessage());

            } catch (IOException ex) {

                System.out.println("I/O error: " + ex.getMessage());
            }
        }
    }
}
