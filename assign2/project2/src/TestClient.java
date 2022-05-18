import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;

// usage: java TestClient <node_ap> <operation> [<opnd>]
public class TestClient {
    public static List<String> operations = Arrays.asList("put", "get", "delete");
    private static String op;
    private static AccessPoint ap;
    private static String opnd;
    private static Integer nArgs;

    public static void main(String[] args) throws UnknownHostException {
        // parsing arguments
        if (args.length > 3 || args.length < 2) {
            throw new IllegalArgumentException("Illegal number of arguments given.\nusage: java TestClient <node_ap> <operation> [<opnd>]");
        }

        nArgs = args.length;
        
        // parse node_ap <ip:port>
        ap = new AccessPoint(args[0]);

        // parse operation
        if (!operations.contains(args[1])) {
            throw new IllegalArgumentException("Invalid operation given. (can only be 'get', 'put', or 'delete')");
        }
        op = args[1];
        
        // when opnd argument is given
        if (args.length == 3) {
            opnd = args[2];
        }

        // starting the tcp server
        try (Socket socket = new Socket(ap.getIp(), ap.getPort())) {

            // send command to server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(getCommand());

            // receive server response to issued commmand
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String time = reader.readLine();
            System.out.println(time);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private static String getCommand(){
        if(nArgs == 2) return op;
        else if(nArgs == 3) return op + " " + opnd;
        return null;
    }
}