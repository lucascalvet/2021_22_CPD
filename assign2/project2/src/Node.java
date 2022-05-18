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

                String time = reader.readLine();

                System.out.println("New client connected: "+ time);



                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);

                writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
