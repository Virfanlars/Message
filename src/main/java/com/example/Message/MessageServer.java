package com.example.Message;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.*;

// Message Server
public class MessageServer {
    private static final int PORT = 12345;
    private static final String MESSAGE_LOG = "messages.log";
    private static final Map<String, String> users = new ConcurrentHashMap<>(); // username -> password
    private static final CopyOnWriteArrayList<Socket> clients = new CopyOnWriteArrayList<>();

    static {
        users.put("user1", "pass1");
        users.put("user2", "pass2");
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Message Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clients.add(clientSocket);
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // Authentication
                out.println("Enter username:");
                String username = in.readLine();
                out.println("Enter password:");
                String password = in.readLine();
                if (!users.containsKey(username) || !users.get(username).equals(password)) {
                    out.println("Authentication failed.");
                    socket.close();
                    return;
                }
                out.println("Authentication successful.");

                String message;
                while ((message = in.readLine()) != null) {
                    String logMessage = username + ": " + message;
                    Files.write(Paths.get(MESSAGE_LOG), (logMessage + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println("Received: " + logMessage);
                    for (Socket client : clients) {
                        if (!client.isClosed() && client != socket) {
                            PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                            clientOut.println(logMessage);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clients.remove(socket);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}