package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.security.TLSConfig;
import javax.net.ssl.SSLContext;

/**
 * TLSTest — smoke test à TLSConfig.
 *
 * Carrega os .jks e constrói os SSLContext de servidor e cliente,
 * confirmando que os ficheiros existem, as passwords estão correctas,
 * e os algoritmos pedidos são suportados pelo JDK.
 *
 * Não envolve rede — só inicialização criptográfica.
 *
 * Responsável: Samuel Ferreira (33846).
 */
public class TLSTest {

    public static void main(String[] args) throws Exception {

        System.out.println("=== TLSTest: validação dos .jks e TLSConfig ===\n");

        SSLContext serverCtx = TLSConfig.buildServerContext();
        System.out.println("[OK] Server SSLContext criado");
        System.out.println("     Protocolo: " + serverCtx.getProtocol());

        SSLContext clientCtx = TLSConfig.buildClientContext();
        System.out.println("[OK] Client SSLContext criado");
        System.out.println("     Protocolo: " + clientCtx.getProtocol());

        System.out.println("\nDefaults usados:");
        System.out.println("  TLS protocol     = " + TLSConfig.TLS_PROTOCOL);
        System.out.println("  Keystore type    = " + TLSConfig.KEYSTORE_TYPE);
        System.out.println("  Server keystore  = " + TLSConfig.DEFAULT_SERVER_KEYSTORE_PATH);
        System.out.println("  Client truststore= " + TLSConfig.DEFAULT_CLIENT_TRUSTSTORE_PATH);

        System.out.println("\n=== TLS configurado correctamente ===");
    }
}