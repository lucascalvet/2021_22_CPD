import protocol.Node;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Store {

    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 4) throw new IllegalArgumentException("Wrong number of arguments.\n" + getUsage());

        InetAddress multicastAddr = InetAddress.getByName(args[0]);
        Integer multicastPort = Integer.valueOf(args[1]);
        String nodeId = args[2];
        Integer storePort = Integer.valueOf(args[3]);

        System.out.println("NODE ID AND PORT: " + nodeId + " " + storePort);
        Node node = new Node(multicastAddr, multicastPort, nodeId, storePort);
        node.run();
    }

    private static String getUsage() {
        return "Usage:" +
                "\tjava Store <IP_mcast_addr> <IP_mcast_port> <node_id>  <Store_port>";
    }
}
