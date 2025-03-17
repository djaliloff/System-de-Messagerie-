import java.io.*;
import java.net.Socket;
public class SMTP_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String sender;
    private String recipient;
    private StringBuilder emailContent;

    public SMTP_Handler(Socket socket) {
        this.clientSocket = socket;
    }

    public void handleClient() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            writer.println("220 Simple SMTP Server Ready");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Commande reçue : " + command);

                if (command.startsWith("HELO") || command.startsWith("EHLO")) {
                    writer.println("250 Hello " + command.split(" ")[1]);
                } else if (command.startsWith("MAIL FROM:")) {
                    sender = command.substring(10).trim();
                    writer.println("250 OK");
                } else if (command.startsWith("RCPT TO:")) {
                    recipient = command.substring(8).trim();
                    writer.println("250 OK");
                } else if (command.equals("DATA")) {
                    writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
                    emailContent = new StringBuilder();

                    String line;
                    while (!(line = reader.readLine()).equals(".")) {
                        emailContent.append(line).append("\n");
                    }

                    MailStorage.saveEmail(recipient, sender ,emailContent.toString());
                    writer.println("250 OK Message received");
                } else if (command.equals("QUIT")) {
                    writer.println("221 Bye");
                    break;
                } else {
                    writer.println("500 Commande non reconnue");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
