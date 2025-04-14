package Servers;

import Servers.Authentification.AuthService;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class POP3_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentUser;
    private List<File> emails;
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
            writer.println((i + 1) + " " + emails.get(i).length());
        }
        writer.println(".");
    }

    private void handleRetr(String command) {
        int index = Integer.parseInt(command.substring(5).trim()) - 1;
        if (index >= 0 && index < emails.size() && !markedForDeletion.get(index)) {
            try {
                writer.println("+OK " + emails.get(index).length() + " octets");
                sendEmail(emails.get(index));
            } catch (IOException e) {
                writer.println("-ERR Error retrieving message");
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
        File userDir = new File("mailserver/" + currentUser);
        if (userDir.exists()) {
            File[] files = userDir.listFiles();
            if (files != null) {
                for (File email : files) {
                    emails.add(email);
                    markedForDeletion.add(false);
                }
            }
        }
    }

    private long getTotalSize() {
        return emails.stream().mapToLong(File::length).sum();
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
        for (int i = 0; i < emails.size(); i++) {
            if (markedForDeletion.get(i)) {
                emails.get(i).delete();
            }
        }
    }
}