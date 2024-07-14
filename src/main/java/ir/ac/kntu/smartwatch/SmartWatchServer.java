package ir.ac.kntu.smartwatch;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class SmartWatchServer {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static final Lock logLock = new ReentrantLock();
    private static final String LOG_FILE = "serverWatch.log";
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Start the server in a new thread
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Smart Watch Server is running on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                threadPool.shutdown();
            }
        }).start();

        // Read commands from the console
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            while ((command = consoleReader.readLine()) != null) {
                handleCommand(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleCommand(String command) {
        if (command.startsWith("poweroff") || command.startsWith("find")) {
            String[] parts = command.split(" ");
            if (parts.length == 2) {
                String imei = parts[1];
                String fullCommand = "[3G*" + imei + "*" + parts[0].toUpperCase() + "]";
                PrintWriter clientOut = clients.get(imei);
                if (clientOut != null) {
                    clientOut.println(fullCommand);
                    logActivity("Sent to client " + imei + ": " + fullCommand);
                } else {
                    System.out.println("Client with IMEI " + imei + " not connected.");
                }
            } else {
                System.out.println("Invalid command format. Use 'poweroff IMEI' or 'find IMEI'.");
            }
        } else {
            System.out.println("Unknown command. Use 'poweroff IMEI' or 'find IMEI'.");
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String imei;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("[3G*") && message.endsWith("]")) {
                        String[] parts = message.substring(4, message.length() - 1).split("\\*");
                        imei = parts[0];
                        clients.put(imei, out); // Register client output stream
                    }
                    logActivity("Received from client: " + message);
                    String response = handleRequest(message);
                    out.println(response);
                    logActivity("Sent to client: " + response);
                }
            } catch (SocketException e) {
                logActivity("Client disconnected: " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (imei != null) {
                    clients.remove(imei); // Remove client on disconnect
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String handleRequest(String message) {
            if (message.startsWith("[3G*") && message.endsWith("]")) {
                String[] parts = message.substring(4, message.length() - 1).split("\\*");
                String imei = parts[0];
                String command = parts[1];

                switch (command) {
                    case "POWEROFF":
                        return "3G*" + imei + "*POWEROFF[OK]";
                    case "FIND":
                        return "3G*" + imei + "*FIND[OK]";
                    case "HEALTH":
                        String healthData = parts[2];
                        logActivity("Health data from " + imei + ": " + healthData);
                        return "3G*" + imei + "*HEALTH[OK]";
                    case "UD":
                        String location = parts[2];
                        logActivity("Location data from " + imei + ": " + location);
                        return "3G*" + imei + "*UD[OK]";
                    default:
                        return "3G*" + imei + "*UNKNOWN[ERROR]";
                }
            }
            return "INVALID_MESSAGE";
        }
    }

    private static void logActivity(String activity) {
        logLock.lock();
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            out.println(timestamp + " - " + activity);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            logLock.unlock();
        }
    }

}
