import processors.client.*;
import utils.Utils;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientProtocol implements Runnable{
    private final Integer port;
    private final String nodeId;
    private final String hashedId;
    private final int NTHREADS = 10;
    private final InetAddress inetAddress;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    ClientProtocol(String nodeId, Integer port) throws UnknownHostException {
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
        this.inetAddress = InetAddress.getByName(nodeId);
    }

    public void run(){

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try (ServerSocket serverSocket = new ServerSocket(port, 50, inetAddress)) {
            System.out.println("Server is listening on port " + port);
            System.out.println("NodeID: " + nodeId);

            while (true) {
                //System.out.println("New Loop");
                Socket socket = serverSocket.accept();
                System.out.println("Accepted New Socket");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String commandLine = reader.readLine();
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                String opArg = null;
                String op = null;
                String[] command = null;
                int replicationFactor = -1;

                if (commandLine.length() == 0) {
                    writer.println("No message given");
                    continue;
                }
                
                if(commandLine.charAt(0) == 'P' || commandLine.charAt(0) == 'G' || commandLine.charAt(0) == 'D'){
                    command = commandLine.split("\\|");
                    if(command.length >= 3) replicationFactor = Integer.parseInt(command[2]);
                }
                else{
                    command = commandLine.split("\\s+");
                }

                op = command[0];
                if(command.length >= 2) opArg = command[1];

                if (command.length == 0) {
                    writer.println("No operation given");
                    continue;
                }

                if (opArg == null){
                    writer.println("No argument given");
                    continue;
                }
                else{
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
                        default:
                            writer.println("Invalid Op");
                    }
                }
                writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
        this.threadPool.shutdown();
    }
}
