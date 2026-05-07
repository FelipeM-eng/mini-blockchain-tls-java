package pt.ipvc.ersc.blockchain.utils;

import java.security.MessageDigest;

public class HashUtil {

    // transforma string em SHA-256
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);

                if (s.length() == 1)
                    hex.append('0');

                hex.append(s);
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}