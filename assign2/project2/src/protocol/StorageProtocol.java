package protocol;

import processors.client.membership.JoinProcessor;
import processors.client.membership.LeaveProcessor;
import processors.client.store.DeleteProcessor;
import processors.client.store.GetProcessor;
import processors.client.store.PutProcessor;
import utils.Utils;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageProtocol implements Runnable {
    private final Node node;
    private final ExecutorService threadPool;

    StorageProtocol(Node node) throws UnknownHostException {
        this.node = node;
        int threadCount = Runtime.getRuntime().availableProcessors();
        threadPool = Executors.newFixedThreadPool(threadCount);
        System.out.println(Thread.currentThread().getName() + ": Created thread pool with " + threadCount + " threads");
    }

    public void run() {

        MembershipNode membershipNode = new MembershipNode(this.node);

        try (ServerSocket serverSocket = new ServerSocket(node.getStorePort(), 50, node.getAddress())) {
            System.out.println("Server is listening on port " + node.getStorePort());
            System.out.println("NodeID: " + node.getNodeId());

            while (true) {
                //System.out.println("ddd");
                Socket socket = serverSocket.accept();
                System.out.println("Accepted New Socket");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String commandLine = reader.readLine();
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String opArg = null;
                String op = null;
                String[] commands = null;
                int replicationFactor = -1;

                if (commandLine.length() == 0) {
                    writer.println("No message given");
                    continue;
                }

                commands = commandLine.split("\\s+", 2);

                if (commands.length == 0) {
                    writer.println("No operation given");
                    continue;
                }

                op = commands[0];
                if (!(op.equals("join") || op.equals("leave"))) {

                    //If the first character is P, G or D we know the message is from another node
                    if (commandLine.charAt(0) == 'P' || commandLine.charAt(0) == 'G' || commandLine.charAt(0) == 'D') {
                        commands = commands[1].split("\\s+", 2);
                        if (commands.length == 0) {
                            writer.println("No operation given");
                            continue;
                        }
                        replicationFactor = Integer.parseInt(commands[0]);
                    }
                    if (commands.length >= 2) opArg = commands[1];

                    String line;
                    while (!(line = reader.readLine()).equals(Utils.MSG_END.substring(1))) {
                        opArg += "\n" + line;
                    }

                    if (opArg == null) {
                        writer.println("No argument given");
                        continue;
                    }
                }

                switch (op) {
                    case "P":
                    case "put":
                        this.threadPool.execute(new PutProcessor(node.getNodeId(), opArg, replicationFactor, node.getStorePort(), socket));
                        break;
                    case "G":
                    case "get":
                        this.threadPool.execute(new GetProcessor(node.getNodeId(), opArg, node.getStorePort(), socket));
                        break;
                    case "D":
                    case "delete":
                        this.threadPool.execute(new DeleteProcessor(node.getNodeId(), opArg, replicationFactor, node.getStorePort(), socket));
                        break;
                    case "join":
                        Thread multicastThread = new Thread(membershipNode, "Multicast Thread");
                        this.threadPool.execute(new JoinProcessor(this.node, socket, multicastThread));
                        break;
                    case "leave":
                        this.threadPool.execute(new LeaveProcessor(this.node, socket, membershipNode));
                        break;
                    default:
                        writer.println("Invalid Op");
                }
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        this.threadPool.shutdown();
    }
}
