package ir.ac.kntu;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileClient implements Runnable {
    private String message;
    private static final Logger logger = Logger.getLogger(FileHandler.class.getName());

    static {
        try {
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler("clientLog.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public FileClient(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket("localhost", 12345)) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            logger.info("Connected to server");

            out.println(message);
            logger.info("Sent: " + message);

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Response: " + response);
            }
        } catch (IOException e) {
            logger.info("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new Thread(new FileClient("read")).start();
            new Thread(new FileClient("write " + i)).start();
            if(i == 0) {
                new Thread(new FileClient("insert 0 inserted text")).start();
            }
            if(i == 1) {
                new Thread(new FileClient("edit 1 new line")).start();
            }
        }
//        new Thread(new FileClient("delete")).start();
    }
}
