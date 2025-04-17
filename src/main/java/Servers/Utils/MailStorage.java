package Servers.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MailStorage {
    public static void saveEmail(String recipient, String sender, String content) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CALL store_email(?, ?, NULL, ?)")) {
            stmt.setString(1, sender);
            stmt.setString(2, recipient);
            stmt.setString(3, content);
            stmt.execute();
        }
    }
}
