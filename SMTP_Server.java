import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
public class SMTP_Server {
        private static final int SMTP_PORT = 25;

        public static void main(String[] args) {
            System.out.println("Serveur SMTP démarré sur le port " + SMTP_PORT);
            try (ServerSocket serverSocket = new ServerSocket(SMTP_PORT)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new SMTP_Handler(clientSocket).handleClient(); // Gère un client à la fois (pas de threads)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
