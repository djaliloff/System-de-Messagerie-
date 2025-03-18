import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class POP3_Handler {
    private Socket clientSocket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentUser;
    private List<File> emails;
    private List<Boolean> markedForDeletion;

    public POP3_Handler(Socket socket) {
        this.clientSocket = socket;
        this.emails = new ArrayList<>();
        this.markedForDeletion = new ArrayList<>();
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
                    String username = command.substring(5).trim();
                    if (userExists(username)) {
                        currentUser = username;
                        writer.println("+OK User recognized");
                    } else {
                        writer.println("-ERR User not found");
                    }
                } else if (command.startsWith("PASS ")) {
                    if (currentUser != null) {
                        writer.println("+OK"); // Toujours OK
                        loadUserEmails();
                    } else {
                        writer.println("-ERR Login first with USER");
                    }
                } else if (command.equals("STAT")) {
                    writer.println("+OK " + emails.size() + " " + getTotalSize());
                } else if (command.equals("LIST")) {
                    writer.println("+OK " + emails.size() + " messages");
                    for (int i = 0; i < emails.size(); i++) {
                        writer.println((i + 1) + " " + emails.get(i).length());
                    }
                    writer.println(".");
                } else if (command.startsWith("RETR ")) {
                    int index = Integer.parseInt(command.substring(5).trim()) - 1;
                    if (index >= 0 && index < emails.size() && !markedForDeletion.get(index)) {
                        writer.println("+OK " + emails.get(index).length() + " octets");
                        sendEmail(emails.get(index));
                    } else {
                        writer.println("-ERR No such message");
                    }
                } else if (command.startsWith("DELE ")) {
                    int index = Integer.parseInt(command.substring(5).trim()) - 1;
                    if (index >= 0 && index < emails.size()) {
                        markedForDeletion.set(index, true);
                        writer.println("+OK Message " + (index + 1) + " deleted");
                    } else {
                        writer.println("-ERR No such message");
                    }
                } else if (command.equals("RSET")) {
                    for (int i = 0; i < markedForDeletion.size(); i++) {
                        markedForDeletion.set(i, false);
                    }
                    writer.println("+OK Reset completed");
                } else if (command.equals("NOOP")) {
                    writer.println("+OK");
                } else if (command.equals("QUIT")) {
                    deleteMarkedEmails();
                    writer.println("+OK POP3 server signing off");
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

    private boolean userExists(String username) {
        File userDir = new File("mailserver/" + username);
        return userDir.exists();
    }

    private void loadUserEmails() {
        emails.clear();
        markedForDeletion.clear();

        File userDir = new File("mailserver/" + currentUser);
        if (userDir.exists() && userDir.isDirectory()) {
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
        long totalSize = 0;
        for (File email : emails) {
            totalSize += email.length();
        }
        return totalSize;
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
