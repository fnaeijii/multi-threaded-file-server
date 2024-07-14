package ir.ac.kntu;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileServer {
    public static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 20;
    private static final BlockingQueue<Socket> requestQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final ReadWriteLock fileLock = new ReentrantReadWriteLock();
    private static final Logger logger = Logger.getLogger(FileServer.class.getName());

    static {
        try {
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler("serverLog.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            Thread requestProcessor = new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = requestQueue.take();
                        threadPool.execute(new FileHandler(clientSocket, fileLock, logger));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            requestProcessor.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                if (!requestQueue.offer(clientSocket)) {
                    System.out.println("Request queue is full, rejecting connection.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
}