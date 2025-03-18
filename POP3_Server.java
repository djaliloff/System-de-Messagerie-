import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class POP3_Server {
    private static final int POP3_PORT = 110;

    public static void main(String[] args) {
        System.out.println("Serveur POP3 démarré sur le port " + POP3_PORT);
        try (ServerSocket serverSocket = new ServerSocket(POP3_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new POP3_Handler(clientSocket).handleClient(); // Gère un client à la fois (pas de threads)
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
