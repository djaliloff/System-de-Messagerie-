//package Servers.Utils;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.SQLException;
//
//public class MigrateEmails {
//    public static void main(String[] args) {
//        File mailDir = new File("src/main/java/Servers/Utils/mailserver/");
//        if (!mailDir.exists()) {
//            System.out.println("No mailserver directory found");
//            return;
//        }
//
//        try (Connection conn = DatabaseUtil.getConnection()) {
//            for (File userDir : mailDir.listFiles()) {
//                if (userDir.isDirectory()) {
//                    String recipient = userDir.getName();
//                    for (File emailFile : userDir.listFiles()) {
//                        try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
//                            String sender = reader.readLine().replace("From: ", "");
//                            String to = reader.readLine().replace("To: ", "");
//                            reader.readLine(); // Skip Date
//                            reader.readLine(); // Skip blank line
//                            StringBuilder content = new StringBuilder();
//                            String line;
//                            while ((line = reader.readLine()) != null) {
//                                content.append(line).append("\n");
//                            }
//                            PreparedStatement stmt = conn.prepareStatement(
//                                    "CALL store_email(?, ?, NULL, ?)");
//                            stmt.setString(1, sender);
//                            stmt.setString(2, recipient);
//                            stmt.setString(3, content.toString());
//                            stmt.execute();
//                        }
//                    }
//                }
//            }
//            System.out.println("Emails migrated successfully");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}