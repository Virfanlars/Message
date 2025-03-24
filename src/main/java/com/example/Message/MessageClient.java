package com.example.Message;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MessageClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        Scanner scanner = new Scanner(System.in);
        System.out.print(in.readLine() + " ");
        out.println(scanner.nextLine());
        System.out.print(in.readLine() + " ");
        out.println(scanner.nextLine());

        System.out.println(in.readLine());

        new Thread(() -> {
            String incomingMessage;
            try {
                while ((incomingMessage = in.readLine()) != null) {
                    System.out.println(incomingMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        String message;
        while (scanner.hasNextLine()) {
            message = scanner.nextLine();
            out.println(message);
        }
    }
}

