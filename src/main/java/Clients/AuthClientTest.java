package Clients;
import Servers.AuthService;

import java.rmi.Naming;

public class AuthClientTest {
    public static void main(String[] args) {
        try {
            AuthService authService = (AuthService) Naming.lookup("rmi://localhost:1099/AuthService");

            boolean loginSuccess = authService.login("alice", "password123");
            System.out.println("Authentification de alice : " + (loginSuccess ? "Réussie" : "Échouée"));

            loginSuccess = authService.login("bob", "wrongpassword");
            System.out.println("Authentification de bob : " + (loginSuccess ? "Réussie" : "Échouée"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

