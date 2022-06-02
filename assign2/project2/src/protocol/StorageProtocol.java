package protocol;

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
    private final Integer port;
    private final String nodeId;
    private final String hashedId;
    private final int NTHREADS = 10;
    private final InetAddress inetAddress;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;
    private InetAddress multicastAddress;
    private Integer multicastPort;

    StorageProtocol(String nodeId, Integer port, InetAddress multicastAddress, Integer multicastPort) throws UnknownHostException {
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.inetAddress = InetAddress.getByName(nodeId);
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    public void run() {

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try (ServerSocket serverSocket = new ServerSocket(port, 50, inetAddress)) {
            System.out.println("Server is listening on port " + port);
            System.out.println("NodeID: " + nodeId);

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
                        this.threadPool.execute(new PutProcessor(nodeId, opArg, replicationFactor, port, writer));
                        break;
                    case "G":
                    case "get":
                        this.threadPool.execute(new GetProcessor(nodeId, opArg, port, writer));
                        break;
                    case "D":
                    case "delete":
                        this.threadPool.execute(new DeleteProcessor(nodeId, opArg, replicationFactor, port, writer));
                        break;
                    case "join":
                        this.threadPool.execute(new JoinProcessor(writer, multicastAddress, multicastPort, nodeId));
                        break;
                    case "leave":
                        this.threadPool.execute(new LeaveProcessor(writer, multicastAddress, multicastPort, nodeId));
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
