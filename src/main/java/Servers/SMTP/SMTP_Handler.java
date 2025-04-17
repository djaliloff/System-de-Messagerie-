package Servers.SMTP;

import Servers.Authentification.AuthService;
import Servers.Utils.MailStorage;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SMTP_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String sender;
    private Set<String> recipients;
    private StringBuilder emailContent;
    private AuthService authService;

    public SMTP_Handler(Socket socket) {
        this.clientSocket = socket;
        this.recipients = new HashSet<>();

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthenticationService");
        } catch (Exception e) {
            System.err.println("RMI Connection Error: " + e.getMessage());
            authService = null;
        }
    }

    public void handleClient() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            writer.println("220 Service ready");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Commande reçue : " + command);

                if (command.startsWith("HELO")) {
                    writer.println("250 OK");
                } else if (command.startsWith("MAIL FROM:")) {
                    handleMailFrom(command);
                } else if (command.startsWith("RCPT TO:")) {
                    handleRcptTo(command);
                } else if (command.equals("DATA")) {
                    handleData();
                } else if (command.equals("RSET")) {
                    handleRset();
                } else if (command.equals("NOOP")) {
                    writer.println("250 OK");
                } else if (command.startsWith("VRFY")) {
                    handleVrfy(command);
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

    private void handleMailFrom(String command) {
        if (isValidMail(command, 10)) {
            sender = cleanEmail(command.substring(10).trim());
            if (authService != null) {
                try {
                    if (authService.userExists(sender)) {
                        writer.println("250 OK");
                    } else {
                        writer.println("550 User unknown");
                    }
                } catch (RemoteException e) {
                    writer.println("451 Authentication service unavailable");
                }
            } else {
                writer.println("451 Authentication service unavailable");
            }
        } else {
            writer.println("501 Syntax error in parameters");
        }
    }

    private void handleRcptTo(String command) {
        if (sender == null) {
            writer.println("503 Bad sequence of commands");
            return;
        }

        if (isValidMail(command, 8)) {
            String recipient = cleanEmail(command.substring(8).trim());
            if (authService != null) {
                try {
                    if (authService.userExists(recipient)) {
                        recipients.add(recipient);
                        writer.println("250 OK");
                    } else {
                        writer.println("550 Recipient unknown");
                    }
                } catch (RemoteException e) {
                    writer.println("451 Authentication service unavailable");
                }
            } else {
                writer.println("451 Authentication service unavailable");
            }
        } else {
            writer.println("501 Syntax error in parameters");
        }
    }

    private void handleData() {
        if (recipients.isEmpty()) {
            writer.println("503 Bad sequence of commands");
            return;
        }

        writer.println("354 Start mail input");
        emailContent = new StringBuilder();
        try {
            String line;
            while (!(line = reader.readLine()).equals(".")) {
                emailContent.append(line).append("\n");
            }
            for (String recipient : recipients) {
                MailStorage.saveEmail(recipient, sender, emailContent.toString());
            }
            writer.println("250 OK Message received");
        } catch (IOException e) {
            writer.println("451 Error storing message");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRset() {
        sender = null;
        recipients.clear();
        emailContent = null;
        writer.println("250 OK Reset state");
    }

    private void handleVrfy(String command) {
        if (isValidMail(command, 5)) {
            String user = cleanEmail(command.substring(5).trim());
            if (authService != null) {
                try {
                    if (authService.userExists(user)) {
                        writer.println("250 OK " + user);
                    } else {
                        writer.println("550 User not found");
                    }
                } catch (RemoteException e) {
                    writer.println("451 Authentication service unavailable");
                }
            } else {
                writer.println("451 Authentication service unavailable");
            }
        } else {
            writer.println("501 Syntax error in parameters");
        }
    }

    private String cleanEmail(String email) {
        return email.replaceAll("[<>]", "");
    }

    private boolean isValidMail(String command, int offset) {
        String email = command.substring(offset).trim();
        return email.matches("<[^@]+@[^@]+\\.[^@]+>");
    }
}