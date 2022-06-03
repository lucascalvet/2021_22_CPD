import rmi.MembershipRmi;
import utils.AccessPoint;
import utils.Utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// usage: java TestClient <node_ap> <operation> [<opnd>]
public class TestClient {
    public static List<String> operations = Arrays.asList("put", "get", "delete", "join", "leave");
    private static String op;
    private static AccessPoint ap;
    private static String opnd;
    private static Integer nArgs;
    private static String CLIENT_LOG_DIRECTORY = "client_logs";

    public static void main(String[] args) throws UnknownHostException, MalformedURLException, RemoteException, NotBoundException {
        // parsing arguments
        if (args.length > 3 || args.length < 2) {
            throw new IllegalArgumentException("Illegal number of arguments given.\nusage: java TestClient <node_ap> <operation> [<opnd>]");
        }

        nArgs = args.length;

        // parse operation
        if (!operations.contains(args[1])) {
            throw new IllegalArgumentException("Invalid operation given. (can only be 'get', 'put', 'delete', 'join', or 'leave')");
        }
        op = args[1];

        if (op.equals("leave") || op.equals("join")) {
            String[] apParts = args[0].split(":");

            String ip = apParts[0];
            String rmiName = apParts[1];

            MembershipRmi rmiStub = (MembershipRmi) Naming.lookup("rmi://" + ip + "/" + rmiName);

            switch (op) {
                case "join":
                    rmiStub.join();
                    break;
                case "leave":
                    rmiStub.leave();
                    break;
                default:
                    break;
            }
        } else {
            // parse node_ap <ip:port>
            ap = new AccessPoint(args[0]);

            // when opnd argument is given
            if (args.length == 3) {
                opnd = args[2];
            }

            System.out.println("Ip: " + ap.getIp());
            System.out.println("Port: " + ap.getPort());
            System.out.println("NArgs: " + nArgs);
            System.out.println("Operation: " + op);
            // starting the tcp server
            try (Socket socket = new Socket(ap.getIp(), ap.getPort())) {

                System.out.println("Socket created");
                // send command to server
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                if (op.equals("put")) {
                    System.out.println("File: " + opnd);
                    String value = Utils.getFileContent(opnd);
                    opnd = value;
                    System.out.println("Value: " + value);
                    System.out.println("Hash: " + Utils.encodeToHex(value));
                }

                writer.println(getCommand());
                // receive server response to issued commmand
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                System.out.println("OUT:");
                String line, answer = "";
                while ((line = reader.readLine()) != null && !(line).equals(Utils.MSG_END_SERVICE)) {
                    answer += line + "\n";
                    System.out.println(line);
                }
                input.close();
                writer.close();
                output.close();
                Utils.makeDir(CLIENT_LOG_DIRECTORY);
                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss'.txt'").format(new Date());
                Utils.writeToFile(CLIENT_LOG_DIRECTORY + File.separator + timeStamp, "REQUEST:\n" + String.join(" ", args) + "\nSERVER ANSWER:\n" + answer + "ENDLOG");
            } catch (UnknownHostException ex) {
                System.out.println("Server not found: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("I/O error: " + ex.getMessage());
            }
        }
    }

    private static String getCommand() {
        if (nArgs == 2) return op;
        else if (nArgs == 3) return op + " " + opnd + "\n" + Utils.MSG_END;
        return null;
    }
}
