package Clients;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientSMTP {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 25);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {

            System.out.println("Connected to SMTP server");
            System.out.println("Server: " + in.nextLine());

            // Example interaction
            out.println("HELO client");
            System.out.println("Server: " + in.nextLine());

            out.println("QUIT");
            System.out.println("Server: " + in.nextLine());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
