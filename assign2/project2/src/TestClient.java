import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

// usage: java TestClient <node_ap> <operation> [<opnd>]
public class TestClient {
    public static List<String> operations = Arrays.asList("put", "get", "delete");
    private static String op;
    private static AccessPoint ap;
    private static String opnd;

    public static void main(String[] args) throws UnknownHostException {
        // parsing arguments
        if (args.length > 3 || args.length < 2) {
            throw new IllegalArgumentException("Illegal number of arguments given.\nusage: java TestClient <node_ap> <operation> [<opnd>]");
        }
        
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
    }
}
