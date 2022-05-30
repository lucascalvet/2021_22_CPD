package processors.client;

import java.io.PrintWriter;

public class JoinProcessor implements Runnable {
    public JoinProcessor(String nodeId, String opArg, Integer port, PrintWriter writer, int counter) {
    }

    @Override
    public void run() {
        // TODO

        // chose port to accept connections

        // multicast >> J << message
    }
}
