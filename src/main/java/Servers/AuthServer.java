package Servers;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuthServer {
    public static void main(String[] args) {
        try {
            AuthServiceImpl authService = new AuthServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("AuthenticationService", authService);
            System.out.println("Authentication RMI Server ready on port 1099");
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}