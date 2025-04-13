package Clients;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientPOP3 {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 110);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {

            System.out.println("Connected to POP3 server");
            System.out.println("Server: " + in.nextLine());

            out.println("USER test");
            System.out.println("Server: " + in.nextLine());

            out.println("PASS test");
            System.out.println("Server: " + in.nextLine());

            out.println("QUIT");
            System.out.println("Server: " + in.nextLine());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
