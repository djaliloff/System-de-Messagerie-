package Servers.Authentification;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import java.nio.file.*;
import java.io.*;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    private static final String USER_DATA_FILE = "Users.json";
    private Map<String, String> users;

    private MessageDigest digest = MessageDigest.getInstance("SHA-256");


    public AuthServiceImpl() throws RemoteException, NoSuchAlgorithmException {
        super();
        digest = MessageDigest.getInstance("SHA-256");
        users = new HashMap<>();
        loadUsersFromFile();
    }

    // Load user data from the JSON file
    private void loadUsersFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(USER_DATA_FILE)));
            JSONObject jsonObject = new JSONObject(content);
            jsonObject.keySet().forEach(key -> users.put(key, jsonObject.getString(key)));
        } catch (IOException e) {
            System.out.println("No user data found, starting fresh.");
        }
    }

    // Save users to the file
    private void saveUsersToFile() {
        try {
            JSONObject jsonObject = new JSONObject(users);
            Files.write(Paths.get(USER_DATA_FILE), jsonObject.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean userExists(String username) throws RemoteException {
        return users.containsKey(username);
    }

    public boolean login(String username, String password) {
        String hashedInput = hash(password);
        return users.get(username).equals(hashedInput);
    }

    private String hash(String password) {
        return Base64.getEncoder().encodeToString(
                digest.digest(password.getBytes())
        );
    }

    @Override
    public boolean createUser(String username, String password) throws RemoteException {
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        users.put(username, hash(password));
        saveUsersToFile();
        return true;
    }

    @Override
    public boolean deleteUser(String username) throws RemoteException {
        if (!users.containsKey(username)) {
            return false; // User does not exist
        }
        users.remove(username);
        saveUsersToFile();
        return true;
    }

    @Override
    public boolean updatePassword(String username, String newPassword) throws RemoteException {
        if (!users.containsKey(username)) {
            return false; // User does not exist
        }
        users.put(username, hash(newPassword));
        saveUsersToFile();
        return true;
    }
}

