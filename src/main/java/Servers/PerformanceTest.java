package Servers.Test;

import Servers.Utils.DatabaseUtil;
import Servers.Utils.MailStorage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTest {
    public static void main(String[] args) {
        // Simuler un grand volume d'emails
        int numberOfEmails = 5000; // Nombre d'emails à insérer
        String sender = "djaout@eoc.dz"; // Sender from users table
        String recipient = "djaout2@eoc.dz"; // Recipient from users table
        String subjectPrefix = "Test Email ";
        String contentPrefix = "This is a test email content. ";

        // Mesurer le temps d'insertion
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfEmails; i++) {
            String subject = subjectPrefix + i;
            String content = "subject: " + subject + "\ncontent: \n" + contentPrefix + i;
            try {
                MailStorage.saveEmail(recipient, sender, content);
            } catch (SQLException e) {
                System.err.println("Failed to store email " + i + ": " + e.getMessage());
            }
        }
        long endTime = System.currentTimeMillis();

        System.out.println("Temps total pour insérer " + numberOfEmails + " emails : " + (endTime - startTime) + " ms");

        // Mesurer le temps de récupération
        startTime = System.currentTimeMillis();
        List<String> emails = fetchEmails(recipient);
        endTime = System.currentTimeMillis();

        System.out.println("Temps total pour récupérer les emails : " + (endTime - startTime) + " ms");
        System.out.println("Nombre d'emails récupérés : " + emails.size());
    }

    private static List<String> fetchEmails(String recipient) {
        List<String> emails = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("CALL fetch_emails(?)")) {
            stmt.setString(1, recipient);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String email = "From: " + rs.getString("sender_email") +
                        ", Subject: " + rs.getString("subject") +
                        ", Content: " + rs.getString("content");
                emails.add(email);
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch emails: " + e.getMessage());
            e.printStackTrace();
        }
        return emails;
    }
}