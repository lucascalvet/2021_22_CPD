package utils;

import java.io.IOException;
import java.net.*;

public class ReceiveMulticast implements Runnable {
    private static String answer = "";
    private final InetAddress multicastAddress;
    private final Integer multicastPort;

    public ReceiveMulticast(InetAddress multicastAddress, Integer multicastPort) throws UnknownHostException {
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    public static String getAnswer() {
        return answer;
    }

    public void run() {
        String multicastMessage = "test";
        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket(multicastPort);
            byte[] buf = new byte[256];

            socket.joinGroup(multicastAddress);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());

                answer += received;

                if ("end".equals(received)) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}