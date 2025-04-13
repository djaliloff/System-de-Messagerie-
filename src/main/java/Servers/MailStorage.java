package Servers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailStorage {
    private static final String STORAGE_DIR = "mailserver/";
    private static final Object fileLock = new Object();

    public static void saveEmail(String recipient, String sender, String content) {
        recipient = recipient.replaceAll("[<>]", ""); // Nettoyage
        synchronized(fileLock) {
            try {
                File userDir = new File(STORAGE_DIR + recipient);
                if (!userDir.exists()) {
                    userDir.mkdirs(); // Création du dossier si nécessaire
                }

                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                File emailFile = new File(userDir, timestamp + ".txt");

                try (FileWriter writer = new FileWriter(emailFile)) {
                    writer.write("From: " + sender + "\n");
                    writer.write("To: " + recipient + "\n");
                    writer.write("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                    writer.write("\n");
                    writer.write(content);
                }

                System.out.println("✅ Email stocké pour " + recipient);
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
    }
}
