package utils;

import java.io.IOException;
import java.net.*;

public class SendMulticast implements Runnable{
    private static String message;
    private InetAddress multicastAddress;
    private Integer multicastPort;

    public SendMulticast(String message, InetAddress multicastAddress, Integer multicastPort) throws UnknownHostException {
        this.message = message;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    public void run(){
        DatagramSocket socket;

        byte[] buf;

        try {
            socket = new DatagramSocket();
            buf = message.getBytes();

            DatagramPacket packet = null;
            packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(multicastAddress.getHostName()), multicastPort);

            socket.send(packet);
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}