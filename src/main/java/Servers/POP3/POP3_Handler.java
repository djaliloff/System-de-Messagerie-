package Servers.POP3;

import Servers.Authentification.AuthService;
import Servers.Utils.DatabaseUtil;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class POP3_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentUser;
    private List<Email> emails;
    private List<Boolean> markedForDeletion;
    private AuthService authService;

    public POP3_Handler(Socket socket) {
        this.clientSocket = socket;
        this.emails = new ArrayList<>();
        this.markedForDeletion = new ArrayList<>();

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

            writer.println("+OK POP3 server ready");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Commande reçue : " + command);

                if (command.startsWith("USER ")) {
                    handleUser(command);
                } else if (command.startsWith("PASS ")) {
                    handlePass(command);
                } else if (command.equals("STAT")) {
                    handleStat();
                } else if (command.equals("LIST")) {
                    handleList();
                } else if (command.startsWith("RETR ")) {
                    handleRetr(command);
                } else if (command.startsWith("DELE ")) {
                    handleDele(command);
                } else if (command.equals("RSET")) {
                    handleRset();
                } else if (command.equals("NOOP")) {
                    writer.println("+OK");
                } else if (command.equals("QUIT")) {
                    handleQuit();
                    break;
                } else {
                    writer.println("-ERR Command not recognized");
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

    private void handleUser(String command) {
        String username = command.substring(5).trim();
        if (authService != null) {
            try {
                if (authService.userExists(username)) {
                    currentUser = username;
                    writer.println("+OK User recognized");
                } else {
                    writer.println("-ERR User not found");
                }
            } catch (RemoteException e) {
                writer.println("-ERR Authentication service unavailable");
            }
        } else {
            writer.println("-ERR Authentication service unavailable");
        }
    }

    private void handlePass(String command) {
        if (currentUser == null) {
            writer.println("-ERR Login first with USER");
            return;
        }

        String password = command.substring(5).trim();
        if (authService != null) {
            try {
                if (authService.login(currentUser, password)) {
                    loadUserEmails();
                    writer.println("+OK Mailbox locked");
                } else {
                    writer.println("-ERR Authentication failed");
                }
            } catch (RemoteException e) {
                writer.println("-ERR Authentication service error");
            }
        } else {
            writer.println("-ERR Authentication service unavailable");
        }
    }

    private void handleStat() {
        writer.println("+OK " + emails.size() + " " + getTotalSize());
    }

    private void handleList() {
        writer.println("+OK " + emails.size() + " messages");
        for (int i = 0; i < emails.size(); i++) {
            writer.println((i + 1) + " " + getEmailSize(emails.get(i)));
        }
        writer.println(".");
    }

    private long getTotalSize() {
        return emails.stream().mapToLong(email -> email.content.length()).sum();
    }

    private long getEmailSize(Email email) {
        return email.content.length();
    }

    private void handleRetr(String command) {
        int index = Integer.parseInt(command.substring(5).trim()) - 1;
        if (index >= 0 && index < emails.size() && !markedForDeletion.get(index)) {
            Email email = emails.get(index);
            writer.println("+OK " + email.content.length() + " octets");
            writer.println(email.content);
            writer.println(".");
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE emails SET is_read = TRUE WHERE email_id = ?")) {
                stmt.setInt(1, email.id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                writer.println("-ERR Database error");
            }
        } else {
            writer.println("-ERR No such message");
        }
    }

    private void handleDele(String command) {
        int index = Integer.parseInt(command.substring(5).trim()) - 1;
        if (index >= 0 && index < emails.size()) {
            markedForDeletion.set(index, true);
            writer.println("+OK Message marked for deletion");
        } else {
            writer.println("-ERR No such message");
        }
    }

    private void handleRset() {
        markedForDeletion.replaceAll(b -> false);
        writer.println("+OK Reset completed");
    }

    private void handleQuit() {
        deleteMarkedEmails();
        writer.println("+OK POP3 server signing off");
    }

    private void loadUserEmails() {
        emails.clear();
        markedForDeletion.clear();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CALL fetch_emails(?)")) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Email email = new Email(
                        rs.getInt("email_id"),
                        rs.getString("sender"),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("content"),
                        rs.getTimestamp("sent_at"),
                        rs.getBoolean("is_read")
                );
                emails.add(email);
                markedForDeletion.add(false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail(File emailFile) throws IOException {
        try (BufferedReader emailReader = new BufferedReader(new FileReader(emailFile))) {
            String line;
            while ((line = emailReader.readLine()) != null) {
                writer.println(line);
            }
            writer.println(".");
        }
    }

    private void deleteMarkedEmails() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            for (int i = 0; i < emails.size(); i++) {
                if (markedForDeletion.get(i)) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "CALL delete_email(?)")) {
                        stmt.setInt(1, emails.get(i).id);
                        stmt.execute();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static class Email {
        int id;
        String sender;
        String recipient;
        String subject;
        String content;
        java.sql.Timestamp sentAt;
        boolean isRead;

        Email(int id, String sender, String recipient, String subject, String content,
              java.sql.Timestamp sentAt, boolean isRead) {
            this.id = id;
            this.sender = sender;
            this.recipient = recipient;
            this.subject = subject;
            this.content = content;
            this.sentAt = sentAt;
            this.isRead = isRead;
        }
    }
}