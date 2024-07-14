package ir.ac.kntu.smartwatch;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class SmartWatchClient implements Runnable {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345; 
    private static final Lock logLock = new ReentrantLock();
    private static final String LOG_FILE = "clientWatch.log";
    private String imei;

    public SmartWatchClient(String imei) {
        this.imei = imei;
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start threads for sending health data, location data, and listening to server commands
            executorService.execute(new HealthDataSender(out, imei));
            executorService.execute(new LocationDataSender(out, imei));
            executorService.execute(new ServerCommandListener(in, out, imei, socket));

            // Wait for all tasks to complete before closing the socket
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void logActivity(String activity) {
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

    public static void main(String[] args) {
        String[] imeis = {"1234567890", "1234567891", "1234567892", "1234567893"};

        for (String imei : imeis) {
            new Thread(new SmartWatchClient(imei)).start();
        }
    }
}

class HealthDataSender implements Runnable {
    private PrintWriter out;
    private String imei;

    public HealthDataSender(PrintWriter out, String imei) {
        this.out = out;
        this.imei = imei;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            try {
                int heartRate = 60 + random.nextInt(40);
                int bpLow = 60 + random.nextInt(20);
                int bpHigh = 100 + random.nextInt(30);
                String message = "[3G*" + imei + "*HEALTH*" + heartRate + "," + bpLow + "," + bpHigh + "]";
                out.println(message);
                SmartWatchClient.logActivity("Sent health data: " + message);
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}

class LocationDataSender implements Runnable {
    private PrintWriter out;
    private String imei;

    public LocationDataSender(PrintWriter out, String imei) {
        this.out = out;
        this.imei = imei;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            try {
                double latitude = 37.7749 + random.nextDouble() * 0.01;
                double longitude = -122.4194 + random.nextDouble() * 0.01;
                String message = "[3G*" + imei + "*UD*" + latitude + "," + longitude + "]";
                out.println(message);
                SmartWatchClient.logActivity("Sent location data: " + message);
                Thread.sleep(45000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}

class ServerCommandListener implements Runnable {
    private BufferedReader in;
    private PrintWriter out;
    private String imei;
    private Socket socket;

    public ServerCommandListener(BufferedReader in, PrintWriter out, String imei, Socket socket) {
        this.in = in;
        this.out = out;
        this.imei = imei;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                SmartWatchClient.logActivity("Received command: " + message);
                if (handleServerCommand(message)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean handleServerCommand(String command) {
        if (command.startsWith("[3G*") && command.endsWith("]")) {
            String[] parts = command.substring(4, command.length() - 1).split("\\*");
            String imei = parts[0];
            String cmd = parts[1];

            if (imei.equals(this.imei)) {
                switch (cmd) {
                    case "POWEROFF":
                        SmartWatchClient.logActivity("Received poweroff command from "+ imei);
                        System.out.println("Powering off{"+imei+"}...");
                        return true; // Exit the loop to close the socket
                    case "FIND":
                        SmartWatchClient.logActivity("Received find command "+ imei);
                        System.out.println("Finding watch{"+imei+"}... Beep beep beep...");
                        break;
                    default:
                        SmartWatchClient.logActivity("Received unknown command: " + command);
                }
            }
        }
        return false;
    }
}
