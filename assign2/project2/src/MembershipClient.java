import processors.client.membership.JoinProcessor;
import processors.client.membership.LeaveProcessor;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MembershipClient implements Runnable {
    private InetAddress multicastAddress;
    private Integer multicastPort;
    private final Integer storePort;
    private final int NTHREADS = 10;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;
    private final String invalidMessage = "InvalidMessage";
    private InetAddress inetAddress;
    private int counter = 0;
    private String nodeId;

    MembershipClient(InetAddress inetAddress, InetAddress multicastAddress, Integer multicastPort, Integer storePort, Integer counter, String nodeId) throws UnknownHostException {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
        this.storePort = storePort;
        this.counter = counter;
        this.inetAddress = inetAddress;
        this.nodeId = nodeId;
    }

    public void run() {
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        // socket to receive commands join and leave from client
        try (ServerSocket serverSocket = new ServerSocket(storePort, 50, inetAddress)) {

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New Socket for commands join and leave from client");

                // read one line of input (because client commands are only one line)
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String commandLine = reader.readLine();
                String[] message = commandLine.split("\\s+");

                // create output stream to answer client with command status
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                // parse command inputted from client
                if (message.length != 1) {
                    writer.println(invalidMessage);
                    throw new IllegalArgumentException("Wrong number of arguments given");
                }
                String operation = message[0];

                switch (operation) {
                    case "join":
                        this.threadPool.execute(new JoinProcessor(writer, multicastAddress, multicastPort, nodeId));
                        break;
                    case "leave":
                        this.threadPool.execute(new LeaveProcessor(writer, multicastAddress, multicastPort, nodeId));
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}