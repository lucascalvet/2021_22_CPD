import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StoreOperations {
    private final Integer port;
    private final String nodeId;
    private final String hashedId;

    StoreOperations(String nodeId, Integer port){
        this.port = port;
        this.nodeId = nodeId;
        this.hashedId = Utils.encodeToHex(nodeId);
    }

    private List<String> readMembers(){
        List<String> nodes = new ArrayList<>();
        String line;
        File membershipLog = new File(hashedId + "\\membership_log.txt");
        try {
            FileReader fr = new FileReader(membershipLog);
            BufferedReader br = new BufferedReader(fr);
            while((line = br.readLine()) != null){
                if(line.length() == Utils.getHashEncodeSize()){
                    nodes.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return nodes;
    }

    public void run(){
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

                if(command.length == 2) opArg = command[1];

                switch (op) {
                    case "put":
                        if (opArg == null){
                            writer.println("No argument given");
                            continue;
                        }
                        else{
                            String value = Utils.getFileContent(opArg);
                            String hash = Utils.encodeToHex(value);
                            String closestNode = Utils.closestNode(readMembers(), hash);

                            System.out.println("File: " + opArg);
                            System.out.println("Value: " + value);
                            System.out.println("Hash: " + hash);

                            //Replication?
                            if(closestNode.equals(hashedId)){
                                Utils.writeToFile(hashedId + "\\storage\\" + hash + ".txt", value, true);
                                writer.println("Pair successfully stored!");
                            }
                            else{
                                System.out.println(hashedId + " is not the closest node to the hash " + hash);
                                //TODO: Send put to the closest node
                            }
                        }
                        break;
                    case "get":
                        if (opArg == null){
                            writer.println("No argument given");
                            continue;
                        }
                        else{
                            String hash = opArg;
                            String closestNode = Utils.closestNode(readMembers(), hash);

                            //Replication?
                            if(Utils.fileExists(hashedId + "\\storage\\" + hash + ".txt")){
                                String value = Utils.getFileContent(hashedId + "\\storage\\" + hash + ".txt");
                                writer.println(value);
                            }
                            else{
                                System.out.println(hashedId + " doesn't have the value for the hash " + hash);
                                //TODO: Send get to the closest node
                            }
                        }
                        break;
                    case "delete":
                        if (opArg == null){
                            writer.println("No argument given");
                            continue;
                        }
                        else{
                            String hash = opArg;
                            String closestNode = Utils.closestNode(readMembers(), hash);

                            //Replication?
                            if(Utils.fileExists(hashedId + "\\storage\\" + hash + ".txt")){
                                if(Utils.deleteFile(hashedId + "\\storage\\" + hash + ".txt")){
                                    writer.println("Pair successfully deleted!");
                                }
                            }
                            else{
                                System.out.println(hashedId + " doesn't have the hash " + hash + " stored");
                                //TODO: Send delete to the closest node
                            }
                        }
                        break;
                    default:
                        writer.println("Invalid Op");
                }

                writer.println(new Date().toString());
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
