import java.io.*;
import java.net.*;
import java.util.Date;

public class Node {

    private final InetAddress multicastAddr;
    private final Integer multicastPort;
    private final String nodeId;
    private final Integer storePort;

    public Node(InetAddress multicastAddr, Integer multicastPort, String nodeId, Integer storePort) {
        this.multicastAddr = multicastAddr;
        this.multicastPort = multicastPort;
        this.nodeId = nodeId;
        this.storePort = storePort;
    }

    public void run() {

        try (ServerSocket serverSocket = new ServerSocket(storePort)) {

            System.out.println("Server is listening on port " + storePort);

            while (true) {
                Socket socket = serverSocket.accept();

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

                if(command.length == 2) opArg = command[1];

                switch (op) {
                    case "put":
                        if (opArg == null){
                            writer.println("No argument given");
                            continue;
                        }
                        break;
                    case "get":
                        break;
                    case "delete":
                        if (opArg == null){
                            writer.println("No argument given");
                            continue;
                        }
                        break;
                    case "join":
                        break;
                    case "leave":
                        break;
                    default:
                        writer.println("Invalid operation given");
                }

                writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
