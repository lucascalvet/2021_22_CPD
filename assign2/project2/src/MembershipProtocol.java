import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MembershipProtocol {
    private final Integer port;
    private final String nodeId;
    private final String hashedId;

    /*
    Messages Format:

    JOIN MESSAGE -> J <node_id> <node_counter>
    LEAVE MESSAGE -> L <node_id> <node_counter>
    MEMBERSHIP MESSAGE -> M <node_id> <node_counter> /n logs_file (each line is a log file line)
     */
    MembershipProtocol(String nodeId, Integer port){
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
    }

    public void run(){
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted New Socket");

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            String commandLine = reader.readLine();

            String[] command = commandLine.split("\\s+");

            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            String op = command[0];
            String opArg = null;

            // checks for right number of arguments in case of J and L
            if(op.equals("J") || op.equals("L") && command.length != 3){
                writer.println("Invalid Op");
            }

            // update membership log
            switch (op){
                case "J":
                    // update membership log
                    String new_log = "join " + command[1] + command[2];
                    Utils.writeToFile(hashedId  + "\\membership_log.txt", new_log, false);
                    break;
                case "L":
                    new_log = "leave " + command[1] + command[2];
                    Utils.writeToFile(hashedId + "\\membership_log.txt", new_log, false);
                    break;
                case "M":

                    break;
                default:
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
