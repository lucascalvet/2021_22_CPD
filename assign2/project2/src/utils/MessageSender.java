package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MessageSender implements Runnable{
    private static AccessPoint ap;
    private static String message;
    private String answer;

    public MessageSender(String ip, Integer port, String message) throws UnknownHostException {
        this.ap = new AccessPoint(ip + ":" + port.toString());
        this.message = message;
        this.answer = "No answer yet!";
    }

    public String getAnswer(){
        return this.answer;
    }

    public void run(){
        try {
            Socket socket = new Socket(ap.getIp(), ap.getPort());
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(message);
            InputStream input = socket.getInputStream();
            this.answer = input.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

