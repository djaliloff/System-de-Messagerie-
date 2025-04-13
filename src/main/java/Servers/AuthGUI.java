package Servers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuthGUI extends JFrame {
    private AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea logArea;
    private JButton  registerButton, updateButton, deleteButton;

    public AuthGUI() {
        initUI();
        setupRMI();
    }

    private void setupRMI() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (AuthService) registry.lookup("AuthenticationService");
            log("Connected to authentication service");
        } catch (Exception e) {
            showError("Connection Error", "Cannot connect to auth service: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initUI() {
        setTitle("Email System Authentication");
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBackground(Color.WHITE);

        usernameField = createSimpleTextField();
        passwordField = createSimplePasswordField();

        inputPanel.add(createSimpleLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(createSimpleLabel("Password:"));
        inputPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        registerButton = createNeutralButton("Register", new Color(70, 130, 180));
        updateButton = createNeutralButton("Update", new Color(169, 169, 169));
        deleteButton = createNeutralButton("Delete", new Color(205, 92, 92));

        buttonPanel.add(registerButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // UI Components
    private JTextField createSimpleTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return field;
    }

    private JPasswordField createSimplePasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return field;
    }

    private JLabel createSimpleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(80, 80, 80));
        return label;
    }

    private JButton createNeutralButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(baseColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });

        button.addActionListener(e -> {
            switch (text) {
                case "Register" -> performRegistration();
                case "Update" -> performUpdate();
                case "Delete" -> performDelete();
            }
        });

        return button;
    }

    private void performRegistration() {
        executeOperation(() -> {
            String username = getUsername();
            String password = getPassword();

            try {
                if (authService.createUser(username, password)) {
                    log("User created: " + username);
                } else {
                    showError("Registration Failed", "Username already exists");
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, "registration");
    }

    private void performUpdate() {
        executeOperation(() -> {
            String username = getUsername();
            String newPassword = getPassword();

            try {
                if (authService.updatePassword(username, newPassword)) {
                    log("Password updated for: " + username);
                } else {
                    showError("Update Failed", "User does not exist");
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, "update");
    }

    private void performDelete() {
        executeOperation(() -> {
            String username = getUsername();

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete user '" + username + "'?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (authService.deleteUser(username)) {
                        showSuccessMessage("User deleted: " + username);
                        clearFields();
                    } else {
                        showError("Deletion Failed", "User does not exist");
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "delete");
    }

    private void executeOperation(Runnable operation, String operationName) {
        if (!validateInput()) return;

        try {
            operation.run();
        } catch (Exception e) {
            log("Error during " + operationName + ": " + e.getMessage());
            showError("Operation Failed", e.getMessage());
        }
    }

    // Utils
    private boolean validateInput() {
        String username = getUsername();
        String password = getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Validation Error", "All fields are required");
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9@._-]{3,30}$")) {
            showError("Validation Error", "Invalid username format");
            return false;
        }

        if (password.length() < 6) {
            showError("Validation Error", "Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private String getUsername() {
        return usernameField.getText().trim();
    }

    private String getPassword() {
        return new String(passwordField.getPassword()).trim();
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
    }

    private void openEmailClient() {
        log("Opening email client...");
        // TODO: Add actual implementation
    }

    private void log(String message) {
        logArea.append(message + "\n");
    }

    private void showErrorDialog(String title, String message, boolean clearPassword) {
        JOptionPane.showMessageDialog(this, "<html><b>" + message + "</b></html>", title, JOptionPane.ERROR_MESSAGE);
        if (clearPassword) passwordField.setText("");
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
        log("Error: " + message);
    }

    private void showSuccessMessage(String message) {
        logArea.setForeground(new Color(0, 100, 0));
        logArea.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AuthGUI().setVisible(true));
    }
}
