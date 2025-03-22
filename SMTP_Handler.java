import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class SMTP_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String sender;
    private Set<String> recipients;
    private StringBuilder emailContent;

    public SMTP_Handler(Socket socket) {
        this.clientSocket = socket;
        this.recipients = new HashSet<>();
    }

    public void handleClient() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            writer.println("220 Service ready");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Commande reçue : " + command);

                if (command.startsWith("HELO") ) {
//                    |EHLO |auth|starttls timeout concurrence
                    writer.println("250 OK");
                } else if (command.startsWith("MAIL FROM:")) {
                    if (isValidMail(command,10)){
                        sender = cleanEmail(command.substring(10).trim());
                        writer.println("250 OK");
                    }else
                        writer.println("501 Syntax error in parameters or arguments");
                } else if (command.startsWith("RCPT TO:")) {
                    if (sender == null || sender.isEmpty()) {
                        writer.println("503 Bad sequence of commands");
                    } else {
                        if (isValidMail(command,8)) {
                            String recipient = cleanEmail(command.substring(8).trim());
                            recipients.add(recipient);
                            writer.println("250 OK " + recipient);
                        }else
                            writer.println("501 Syntax error in parameters or arguments");
                    }
                } else if (command.equals("DATA")) {
                    if (recipients.isEmpty()) {
                        writer.println("503 Bad sequence of commands");
                        continue;
                    }
                    writer.println("354 Start mail input; end with <CRLF>.<CRLF>");
                    emailContent = new StringBuilder();
                    String line;
                    while (!(line = reader.readLine()).equals(".")) {
                        emailContent.append(line).append("\n");
                    }
                    for (String recipient : recipients) {
                        MailStorage.saveEmail(recipient, sender, emailContent.toString());
                    }
                    writer.println("250 OK Message received");
                } else if (command.equals("RSET")) {
                    sender = null;
                    recipients.clear();
                    emailContent = null;
                    writer.println("250 OK Reset state");
                } else if (command.equals("NOOP")) {
                    writer.println("250 OK");
                } else if (command.startsWith("VRFY")) {
                    if (isValidMail(command,8)) {
                        String user = cleanEmail(command.substring(5).trim());
                        if (verifyUser(user)) {
                            writer.println("250 OK " + user);
                        } else {
                            writer.println("550 User not found");
                        }
                    }else
                        writer.println("501 Syntax error in parameters or arguments");
                } else if (command.equals("QUIT")) {
                    writer.println("221 Service closing transmission channel");
                    break;
                } else {
                    writer.println("500 Syntax error, command unrecognized");
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

    private boolean verifyUser(String email) {
        return new File("mailserver/" + email).exists();
    }


    private String cleanEmail(String email) {
        return email.replaceAll("[<>]", ""); // Supprime les chevrons < >
    }

    private boolean isValidMail(String command,int deb) {
        String email = command.substring(deb).trim();
        return email.matches("<[^@]+@[^@]+\\.[^@]+>");
    }
}
