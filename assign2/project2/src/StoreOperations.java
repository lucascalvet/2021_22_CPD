import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreOperations implements Runnable{
    private final Integer port;
    private final String nodeId;
    private final String hashedId;
    private final int NTHREADS = 10;
    private ExecutorService threadPool = Executors.newFixedThreadPool(NTHREADS);
    private Thread runningThread = null;

    StoreOperations(String nodeId, Integer port){
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
    }

    public void run(){
        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);
            System.out.println("NodeID: " + nodeId);

            while (true) {
                //System.out.println("New Loop");
                Socket socket = serverSocket.accept();
                System.out.println("Accepted New Socket");

                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                String commandLine = reader.readLine();

                String[] command = commandLine.split("\\s+");

                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                if (command.length == 0) {
                    writer.println("No operation given");
                    continue;
                }

                String op = command[0];
                String opArg = null;
                int replicationFactor = -1;

                if(command.length >= 2) opArg = command[1];
                if(command.length == 3) replicationFactor = Integer.valueOf(command[2]);

                if (opArg == null){
                    writer.println("No argument given");
                    continue;
                }
                else{
                    switch (op) {
                        case "put":
                            this.threadPool.execute(new PutProcessor(nodeId, opArg, replicationFactor, port, writer));
                            break;
                        case "get":
                            this.threadPool.execute(new GetProcessor(nodeId, opArg, port, writer));
                            break;
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
