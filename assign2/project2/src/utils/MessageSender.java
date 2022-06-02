package utils;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class MessageSender implements Runnable{
    private static AccessPoint ap;
    private static String message;
    private String answer;

    public MessageSender(String ip, Integer port, String message) throws UnknownHostException {
        this.ap = new AccessPoint(ip + ":" + port.toString());
        this.message = message + Utils.MSG_END;
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
            //this.answer = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            this.answer = reader.readLine();
            String line;
            while (!(line = reader.readLine()).equals(Utils.MSG_END_SERVICE)) {
                this.answer += "\n" + line;
            }
            //this.answer += "\n" + Utils.MSG_END_SERVICE;
            this.answer += "\n" + Utils.MSG_END_SERVICE;
            System.out.println("ANS: " + answer);
            input.close();
            writer.close();
            output.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

