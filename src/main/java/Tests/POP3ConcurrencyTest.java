package Tests;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class POP3ConcurrencyTest {
    private static final int CLIENT_COUNT = 3;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int POP3_PORT = 110;

    public static void main(String[] args) {
        for (int i = 0; i < CLIENT_COUNT; i++) {
            int clientId = i + 1;
            new Thread(() -> runPOP3Client(clientId)).start();
        }
    }

    private static void runPOP3Client(int clientId) {
        try (Socket socket = new Socket(SERVER_ADDRESS, POP3_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {

            System.out.println("Client " + clientId + " connecté.");

            System.out.println("Client " + clientId + " > " + in.nextLine()); // Message de bienvenue du serveur

            out.println("USER user" + clientId);
            System.out.println("Client " + clientId + " > " + in.nextLine());

            out.println("PASS password" + clientId);
            System.out.println("Client " + clientId + " > " + in.nextLine());

            out.println("STAT");
            System.out.println("Client " + clientId + " > " + in.nextLine());

            out.println("LIST");
            System.out.println("Client " + clientId + " > " + in.nextLine());

            out.println("RETR 1");
            System.out.println("Client " + clientId + " > " + in.nextLine());

            out.println("QUIT");
            System.out.println("Client " + clientId + " > " + in.nextLine());

            System.out.println("Client " + clientId + " déconnecté.");

        } catch (Exception e) {
            System.err.println("Client " + clientId + " erreur : " + e.getMessage());
        }
    }
}
