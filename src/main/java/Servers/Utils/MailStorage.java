package Servers.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MailStorage {
    public static void saveEmail(String recipient, String sender, String content) throws SQLException {
        System.out.println("Attempting to save email: recipient_id=" + recipient + ", sender_id=" + sender + ", content=" + content);

        // Fetch sender_id and recipient_id
        int senderId = getUserId(sender);
        int recipientId = getUserId(recipient);

        if (senderId == -1 || recipientId == -1) {
            throw new SQLException("User not found: sender_id=" + sender + ", recipient_id=" + recipient);
        }

        // Parse subject and content
        String subject = "";
        String body = content;
        String[] lines = content.split("\n", 2);
        if (lines.length > 0 && lines[0].toLowerCase().startsWith("subject: ")) {
            subject = lines[0].substring("subject: ".length()).trim();
            if (lines.length > 1) {
                // Look for "content:" in the remaining lines
                String[] remainingLines = lines[1].split("\n", 2);
                if (remainingLines.length > 0 && remainingLines[0].toLowerCase().startsWith("content: ")) {
                    body = remainingLines.length > 1 ? remainingLines[1].trim() : "";
                } else {
                    body = lines[1].trim();
                }
            } else {
                body = "";
            }
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO emails (sender_id, recipient_id, subject, content) VALUES (?, ?, ?, ?)")) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, recipientId);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Email saved successfully, rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQLException in saveEmail: " + e.getMessage());
            throw e;
        }
    }

    private static int getUserId(String email) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT user_id FROM users WHERE email = ? OR username = ?")) {
            stmt.setString(1, email);
            stmt.setString(2, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        }
        return -1; // User not found
    }
}
