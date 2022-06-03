package protocol;

import processors.client.membership.JoinProcessor;
import processors.client.membership.LeaveProcessor;
import processors.client.membership.RMIProcessor;
import processors.client.membership.ReconnectProcessor;
import processors.client.store.DeleteProcessor;
import processors.client.store.GetProcessor;
import processors.client.store.PutProcessor;

import rmi.MembershipRmi;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;

public class StorageProtocol implements Runnable {
    private final Node node;
    private final ExecutorService threadPool;
    private final MembershipNode membershipNode;

    StorageProtocol(Node node) {
        this.node = node;
        this.threadPool = this.node.getThreadPool();
        this.membershipNode = new MembershipNode(this.node);
    }

    public void run() {
        if (node.getCounter() % 2 == 0) {
            new Thread(membershipNode, "Multicast Thread").start();
        }

        RMIProcessor rmiProcessor = new RMIProcessor(node, membershipNode);

        try {
            MembershipRmi stub = (MembershipRmi) UnicastRemoteObject.exportObject(rmiProcessor, 0);

            // Bind the remote object's stub in the registry
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(node.getNodeId());
            }
            registry.rebind(node.getNodeId(), stub);

            System.err.println("RMI server ready");
        } catch (Exception e) {
            System.err.println("RMI server exception: " + e.getMessage());
            e.printStackTrace();
        }

        try (ServerSocket serverSocket = new ServerSocket(node.getStorePort(), 50, node.getAddress())) {
            System.out.println("Server is listening on port " + node.getStorePort());
            System.out.println("NodeID: " + node.getNodeId());

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Accepted New Socket");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String commandLine = reader.readLine();
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String opArg = null;
                String op;
                String[] commands;
                int factor = -1;
                boolean force = false;

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
                    if (commandLine.charAt(0) == 'F' || commandLine.charAt(0) == 'P' || commandLine.charAt(0) == 'G' || commandLine.charAt(0) == 'D') {
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
                    case "F":
                        force = true;
                    case "P":
                    case "put":
                        this.threadPool.execute(new PutProcessor(this.node, opArg, factor, force, socket));
                        break;
                    case "G":
                    case "get":
                        this.threadPool.execute(new GetProcessor(this.node, opArg, factor, socket));
                        break;
                    case "D":
                    case "delete":
                        this.threadPool.execute(new DeleteProcessor(this.node, opArg, factor, socket));
                        break;
                    case "join":
                        this.threadPool.execute(new JoinProcessor(this.node, membershipNode, socket));
                        break;
                    case "leave":
                        this.threadPool.execute(new LeaveProcessor(this.node, membershipNode, socket));
                        break;
                    default:
                        writer.println("Invalid Op");
                }

                if (node.getNeedsReconnect()) {
                    this.threadPool.execute(new ReconnectProcessor(this.node, membershipNode));
                }
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        this.threadPool.shutdown();
    }
}
