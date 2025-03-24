package com.example.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// Message Server with Authentication and Persistence
public class MessageServer {
    private static final int PORT = 12345;
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final Map<String, Socket> onlineUsers = new ConcurrentHashMap<>();
    private static final Map<String, Queue<String>> offlineMessages = new ConcurrentHashMap<>();
    private static final String USERS_FILE = "users.txt";
    private static final String MESSAGES_FILE = "messages.log";

    public static void main(String[] args) throws IOException {
        loadUsers();
        loadOfflineMessages();
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Message Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static void loadUsers() throws IOException {
        File file = new File(USERS_FILE);
        if (!file.exists()) file.createNewFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    users.put(parts[0], parts[1]);
                }
            }
        }
    }

    private static void loadOfflineMessages() throws IOException {
        File file = new File(MESSAGES_FILE);
        if (!file.exists()) file.createNewFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                offlineMessages.computeIfAbsent(parts[0], k -> new LinkedList<>()).add(parts[1]);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;

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
                this.username = username;
                onlineUsers.put(username, socket);
                out.println("Welcome, " + username);

                // Send offline messages
                Queue<String> messages = offlineMessages.getOrDefault(username, new LinkedList<>());
                while (!messages.isEmpty()) {
                    out.println("Offline: " + messages.poll());
                }

                // Message handling
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        String[] parts = message.split(" ", 2);
                        String targetUser = parts[0].substring(1);
                        String msg = parts.length > 1 ? parts[1] : "";
                        if (onlineUsers.containsKey(targetUser)) {
                            new PrintWriter(onlineUsers.get(targetUser).getOutputStream(), true).println(username + ": " + msg);
                        } else {
                            offlineMessages.computeIfAbsent(targetUser, k -> new LinkedList<>()).add(username + ": " + msg);
                            try (FileWriter fw = new FileWriter(MESSAGES_FILE, true)) {
                                fw.write(targetUser + ":" + username + ": " + msg + "\n");
                            }
                        }
                    } else {
                        for (Socket client : onlineUsers.values()) {
                            if (client != socket) {
                                new PrintWriter(client.getOutputStream(), true).println(username + ": " + message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (username != null) {
                    onlineUsers.remove(username);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}