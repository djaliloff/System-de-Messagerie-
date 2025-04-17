package Servers.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String URL = "jdbc:mariadb://localhost:3306/messaging_system?useSSL=false";
    private static final String USER = "root"; // XAMPP default user
    private static final String PASSWORD = ""; // XAMPP default: empty password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}