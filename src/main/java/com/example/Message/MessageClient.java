package com.example.Message;
import java.io.*;
import java.net.*;

public class MessageClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, PORT);
        System.out.println("Connected to server.");

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

        // Authentication
        System.out.print(in.readLine() + " ");
        out.println(consoleInput.readLine());
        System.out.print(in.readLine() + " ");
        out.println(consoleInput.readLine());
        String authResult = in.readLine();
        System.out.println(authResult);
        if (!authResult.equals("Authentication successful.")) {
            socket.close();
            return;
        }

        new Thread(() -> {
            try {
                String incomingMessage;
                while ((incomingMessage = in.readLine()) != null) {
                    System.out.println("Message from server: " + incomingMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        String message;
        while ((message = consoleInput.readLine()) != null) {
            out.println(message);
        }
    }
}
