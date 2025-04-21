package Servers.Authentification;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;
import Servers.Utils.DatabaseUtil;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    public AuthServiceImpl() throws RemoteException, NoSuchAlgorithmException {
        super();
    }

    @Override
    public boolean userExists(String identifier) throws RemoteException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?")) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error", e);
        }
        return false;
    }

    @Override
    public boolean login(String identifier, String password) throws RemoteException {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT password FROM users WHERE username = ? OR email = ?")) {
            stmt.setString(1, identifier);
            stmt.setString(2, identifier);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return BCrypt.checkpw(password, hashedPassword);
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error", e);
        }
        return false;
    }

    @Override
    public boolean createUser(String username,String email, String password) throws RemoteException {
        if (userExists(username)) {
            return false;
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, email, password, clear_password) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public boolean deleteUser(String username) throws RemoteException {
        if (!userExists(username)) {
            return false;
        }
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RemoteException("Database error", e);
        }
    }

    @Override
    public boolean updatePassword(String username, String newPassword) throws RemoteException {
        if (!userExists(username)) {
            return false;
        }
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE username = ?")) {
            stmt.setString(1, hashedPassword);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RemoteException("Database error", e);
        }
    }
    public static void main(String[] args) {
        try {
            AuthServiceImpl service = new AuthServiceImpl();
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.createRegistry(1099);
            registry.rebind("AuthenticationService", service);
            System.out.println("RMI Authentication Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

