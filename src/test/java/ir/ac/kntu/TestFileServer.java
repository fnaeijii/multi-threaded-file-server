package ir.ac.kntu;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileServer {
    private static final int PORT = 12345;
    private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    @BeforeAll
    public static void setUp() {
        threadPool.submit(() -> FileServer.main(new String[]{}));
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void testReadOperation() throws IOException {
        String readStr = "";
        try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                readStr += line + "\n";
            }
        }

        sendCommand("read");
        String serverResponse = receiveResponse();
        System.out.println(serverResponse);

        assertNotNull(serverResponse);
        assertTrue(serverResponse.contains(readStr));
    }

    @Test
    public void testWriteOperation() throws IOException {
        String content = "This is a test write";
        sendCommand("write " + content);

        try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
            String lastLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine += line;
            }
            assertNotNull(lastLine);
            assertTrue(lastLine.contains(content));
        }
    }

    @Test
    public void testEditOperation() throws IOException {
        String newContent = "Edited content";
        sendCommand("edit 0 " + newContent);

        try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
            String line = reader.readLine();
            System.out.println(line);
            assertNotNull(line);
            assertTrue(line.contains(newContent));
        }
    }

    @Test
    public void testInsertOperation() throws IOException {
        String content = "Inserted content";
        sendCommand("insert 0 " + content);

        try (BufferedReader reader = new BufferedReader(new FileReader("data.txt"))) {
            String firstLine = reader.readLine();
            System.out.println(firstLine);
            assertNotNull(firstLine);
            assertTrue(firstLine.contains(content));
        }
    }

    @Test
    public void testDeleteOperation() throws IOException {
        sendCommand("delete");

        assertFalse(Files.exists(Paths.get("data.txt")));
    }

    private void sendCommand(String command) throws IOException {
        try (Socket socket = new Socket("localhost", PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(command);
        }
    }

    private String receiveResponse() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT);
             Socket clientSocket = serverSocket.accept();
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
             String str, finalStr = "";
             while ((str = in.readLine()) != null) {
                 finalStr += str + "\n";
             }
            return finalStr;
        }
    }
}
