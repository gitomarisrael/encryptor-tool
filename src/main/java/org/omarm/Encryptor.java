package org.omarm;

import java.io.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {

    private static final String SECRET_KEY = initSecretKey();

    private static String initSecretKey() {
        String envKey = System.getenv("ENCRYPTION_KEY");

        if (envKey != null && (envKey.length() == 16 || envKey.length() == 24 || envKey.length() == 32)) {
            return envKey;
        }


        // ⚠️ Clave por default SOLO para desarrollo
        String defaultKey = "defaultdevsecret"; // 16 caracteres exactos
        System.out.println("⚠️ ADVERTENCIA: Usando clave de encriptación por defecto (solo para desarrollo)");
        return defaultKey;
    }


    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: java -jar encryptor.jar <archivo_csv> <archivo_salida_yaml>");
            System.exit(1);
        }

        String csvPath = args[0];
        String outputPath = args[1];

        if (SECRET_KEY == null || !(SECRET_KEY.length() == 16 || SECRET_KEY.length() == 24 || SECRET_KEY.length() == 32)) {
            System.err.println("ERROR: La variable de entorno ENCRYPTION_KEY debe estar definida y tener 16, 24 o 32 caracteres.");
            System.exit(1);
        }

        File csvFile = new File(csvPath);
        if (!csvFile.exists() || csvFile.length() == 0) {
            System.err.println("ERROR: El archivo CSV no existe o está vacío.");
            System.exit(1);
        }

        StringBuilder yaml = new StringBuilder();
        int validLines = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length != 2) continue; // Ignora líneas mal formadas

                String key = parts[0].trim();
                String value = parts[1].trim();

                String encryptedValue = encrypt(value);
                yaml.append(key)
                        .append(": \"![")
                        .append(encryptedValue)
                        .append("]\"\n");

                validLines++;
            }
        }

        if (validLines == 0) {
            System.err.println("ERROR: El archivo CSV no contiene líneas válidas con formato key,value.");
            System.exit(1);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
            bw.write(yaml.toString());
        }

        System.out.println("Archivo YAML generado correctamente: " + outputPath);
    }


    private static String encrypt(String strToEncrypt) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
    }
}
