package Servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class POP3_Server {
    private static final int POP3_PORT = 110;
    private static final int MAX_THREADS = 11;

    public static void main(String[] args) {
        System.out.println("Serveur POP3 démarré sur le port " + POP3_PORT);
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        try (ServerSocket serverSocket = new ServerSocket(POP3_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(300000);
                new Thread(() -> {
                    threadPool.execute(() -> new POP3_Handler(clientSocket).handleClient());
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
