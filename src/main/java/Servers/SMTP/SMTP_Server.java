package Servers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SMTP_Server {
        private static final int SMTP_PORT = 25;
        private static final int MAX_THREADS = 4;

        public static void main(String[] args) {
            System.out.println("Serveur SMTP démarré sur le port " + SMTP_PORT);
            ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
            try (ServerSocket serverSocket = new ServerSocket(SMTP_PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(300000);
                    new Thread(() -> {
                        threadPool.execute(() -> new SMTP_Handler(clientSocket).handleClient());
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
