package Servers.Utils;

import Servers.Utils.DatabaseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MigrateUsers {
    private static final Logger logger = LoggerFactory.getLogger(MigrateUsers.class);

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Load users.json from the classpath
            InputStream jsonInputStream = MigrateUsers.class.getClassLoader().getResourceAsStream("users.json");
            if (jsonInputStream == null) {
                logger.error("No users.json found in classpath");
                return;
            }

            JsonNode root = mapper.readTree(jsonInputStream);
            if (root == null || !root.isObject()) {
                logger.error("Invalid JSON format in users.json; expected an object");
                return;
            }

            // Use INSERT IGNORE to skip duplicates
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT IGNORE INTO users (username, email, password) VALUES (?, ?, ?)");
            root.fields().forEachRemaining(entry -> {
                String email = entry.getKey();
                String password = entry.getValue().asText();

                if (email == null || password == null) {
                    logger.warn("Skipping entry with missing fields: email={}, password={}", email, password);
                    return;
                }

                try {
                    stmt.setString(1, email); // Use email as username
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                try {
                    stmt.setString(2, email); // Use email as email
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                try {
                    stmt.setString(3, password); // Use provided hashed password
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                try {
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        logger.info("Migrated user: email={}", email);
                    } else {
                        logger.warn("User already exists, skipped: email={}", email);
                    }
                } catch (SQLException e) {
                    logger.error("Failed to migrate user {}: {}", email, e.getMessage());
                }
            });
            logger.info("Users migration completed");
        } catch (Exception e) {
            logger.error("Migration failed", e);
        }
    }
}