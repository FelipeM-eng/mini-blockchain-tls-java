package pt.ipvc.ersc.blockchain.utils;

import java.security.MessageDigest;

/**
 * Classe HashUtil — utilitário para calcular hashes SHA-256.
 * Responsabilidades:
 * - Transformar strings em hashes SHA-256 com digest.
 * - Garantir integridade e unicidade de dados com a classe MessageDigest.
 * - Prevenir adulteração e colisões de hash com a classe StringBuilder.
 */
public class HashUtil {

    // transforma string em SHA-256
    public static String sha256(String input) {
        try {
            // Dado que pedimos para o Java usar o algoritmo SHA-256 para calcular o hash da string. da API de segurança do Java.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Quando passamos a string para o algoritmo, ele a converte em bytes.
            byte[] hash = digest.digest(input.getBytes("UTF-8"));

            // e criamos um StringBuilder para armazenar o hash em formato hexadecimal.
            StringBuilder hex = new StringBuilder();

            // para cada byte  no hash, é guardado em byte b.
            for (byte b : hash) {
                // convertemos cada byte em um hexadecimal.
                String s = Integer.toHexString(0xff & b);

                // se o byte for apenas 1 caracter, então adicionamos um 0 a esquerda.
                if (s.length() == 1)
                    hex.append('0');

                // e adicionamos o byte convertido em hexadecimal ao StringBuilder.
                hex.append(s);
            }
            // e retornamos o hash em formato hexadecimal.
            return hex.toString();

        // caso ocorra um erro, então lançamos uma exceção.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}