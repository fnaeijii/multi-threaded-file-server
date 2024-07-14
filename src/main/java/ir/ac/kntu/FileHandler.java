package ir.ac.kntu;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;

public class FileHandler implements Runnable {
    private Socket clientSocket;
    private ReadWriteLock fileLock;
    private Logger logger;

    public FileHandler(Socket clientSocket, ReadWriteLock fileLock, Logger logger) {
        this.clientSocket = clientSocket;
        this.fileLock = fileLock;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                logger.info("Received from " + Thread.currentThread().getName() + ": " + message);
                if (message.startsWith("read")) {
                    readFile(out);
                } else if (message.startsWith("write")) {
                    writeFile(message, out);
                } else if (message.startsWith("delete")) {
                    deleteFile(out);
                } else if (message.startsWith("insert")) {
                    insertToFile(message, out);
                } else if (message.startsWith("edit")) {
                    editFile(message, out);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void readFile(PrintWriter out) {
        fileLock.readLock().lock();
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader("data.txt"));
            String line;
            while ((line = fileReader.readLine()) != null) {
                out.println(line);
            }
            fileReader.close();
            logger.info(Thread.currentThread().getName() + " completed read operation");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.readLock().unlock();
        }
    }

    void writeFile(String message, PrintWriter out) {
        fileLock.writeLock().lock();
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter("data.txt", true));
            fileWriter.write(message.substring(6) + "\n");
            fileWriter.close();
            out.println(message + " successful");
            logger.info(Thread.currentThread().getName() + " completed write operation");
        } catch (IOException e) {
            out.println("Write failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    void deleteFile(PrintWriter out) {
        fileLock.writeLock().lock();
        try {
            Files.deleteIfExists(Paths.get("data.txt"));
            out.println("File deleted successfully");
            logger.info(Thread.currentThread().getName() + " completed delete operation");
        } catch (IOException e) {
            out.println("Failed to delete file: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    void insertToFile(String message, PrintWriter out) {
        fileLock.writeLock().lock();
        try {
            String[] parts = message.split(" ", 3);
            int position = Integer.parseInt(parts[1]);
            String content = parts[2];

            File file = new File("data.txt");
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(position);
            long length = file.length() - position;
            byte[] buffer = new byte[(int) length];
            raf.readFully(buffer);
            raf.seek(position);
            raf.write(content.getBytes());
            raf.write(buffer);
            raf.close();

            out.println("Content inserted successfully");
            logger.info(Thread.currentThread().getName() + " completed insert operation");
        } catch (IOException e) {
            out.println("Failed to insert content: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    void editFile(String message, PrintWriter out) {
        fileLock.writeLock().lock();
        try {
            String[] parts = message.split(" ", 3);
            int lineNumber = Integer.parseInt(parts[1]);
            String newContent = parts[2];

            File file = new File("data.txt");
            File tempFile = new File("data_temp.txt");

            BufferedReader reader = new BufferedReader(new FileReader(file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;
            int currentLineNumber = 0;

            while ((currentLine = reader.readLine()) != null) {
                if (currentLineNumber == lineNumber) {
                    writer.write(newContent + System.lineSeparator());
                } else {
                    writer.write(currentLine + System.lineSeparator());
                }
                currentLineNumber++;
            }

            reader.close();
            writer.close();

            if (!file.delete()) {
                out.println("Failed to delete original file");
                return;
            }

            if (!tempFile.renameTo(file)) {
                out.println("Failed to rename temp file");
            } else {
                out.println("File edited successfully");
                logger.info(Thread.currentThread().getName() + " completed edit operation");
            }
        } catch (IOException e) {
            out.println("Failed to edit file: " + e.getMessage());
        } finally {
            fileLock.writeLock().unlock();
        }
    }
}
