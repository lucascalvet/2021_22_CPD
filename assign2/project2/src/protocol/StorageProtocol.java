package protocol;

import processors.client.membership.JoinProcessor;
import processors.client.membership.LeaveProcessor;
import processors.client.store.DeleteProcessor;
import processors.client.store.GetProcessor;
import processors.client.store.PutProcessor;

import rmi.MembershipRmi;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;

public class StorageProtocol implements Runnable, MembershipRmi {
    private Node node = null;
    private final ExecutorService threadPool;
    private final MembershipNode membershipNode = new MembershipNode(this.node);
    private Socket socket;

    StorageProtocol(Node node) {
        this.node = node;
        this.threadPool = this.node.getThreadPool();
    }

    public void run() {
        MembershipNode membershipNode = new MembershipNode(this.node);
        Thread multicastThread = new Thread(membershipNode, "Multicast Thread");

        if (node.getCounter() % 2 == 0) {
            multicastThread.start();
        }

        try (ServerSocket serverSocket = new ServerSocket(node.getStorePort(), 50, node.getAddress())) {
            System.out.println("Server is listening on port " + node.getStorePort());
            System.out.println("NodeID: " + node.getNodeId());

            while (true) {
                this.socket = serverSocket.accept();
                System.out.println("Accepted New Socket");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String commandLine = reader.readLine();
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String opArg = null;
                String op = null;
                String[] commands = null;
                int factor = -1;

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
                    //Storage Message->"OP factor Value\nEND"
                    //If the first character is P, G or D we know the message is from another node
                    if (commandLine.charAt(0) == 'P' || commandLine.charAt(0) == 'G' || commandLine.charAt(0) == 'D') {
                        commands = commands[1].split("\\s+", 2);
                        if (commands.length == 0) {
                            writer.println("No operation given");
                            continue;
                        }
                        factor = Integer.parseInt(commands[0]);
                    }
                    if (commands.length >= 2) opArg = commands[1];

                    String line;
                    while (!(line = reader.readLine()).equals(node.getMSG_END())) {
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
                        this.threadPool.execute(new PutProcessor(this.node, opArg, factor, socket));
                        break;
                    case "G":
                    case "get":
                        this.threadPool.execute(new GetProcessor(this.node, opArg, factor, socket));
                        break;
                    case "D":
                    case "delete":
                        this.threadPool.execute(new DeleteProcessor(this.node, opArg, factor, socket));
                        break;
                        /*
                    case "join":
                        this.threadPool.execute(new JoinProcessor(this.node, socket, multicastThread));
                        break;
                    case "leave":
                        this.threadPool.execute(new LeaveProcessor(this.node, socket, membershipNode));
                        break;
                         */
                    default:
                        writer.println("Invalid Op");
                }

                if (node.getNeedsReconnect()) {
                    this.threadPool.execute(new LeaveProcessor(this.node, socket, membershipNode));
                    this.threadPool.execute(new JoinProcessor(this.node, socket, multicastThread));
                }
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        this.threadPool.shutdown();
    }

    @Override
    public void leave() throws RemoteException {
        this.threadPool.execute(new LeaveProcessor(this.node, socket, membershipNode));
    }

    @Override
    public void join() throws RemoteException {
        Thread multicastThread = new Thread(membershipNode, "Multicast Thread");
        this.threadPool.execute(new JoinProcessor(this.node, socket, multicastThread));
    }
}
