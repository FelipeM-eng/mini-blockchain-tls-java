package pt.ipvc.ersc.blockchain.security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * TLSConfig — fábrica centralizada de contextos TLS para o projeto.
 *
 * Responsabilidades:
 *  - Carregar o keystore do servidor (chave privada + certificado X.509).
 *  - Carregar o truststore do cliente (certificados em quem confia).
 *  - Construir SSLContext para o lado servidor e para o lado cliente.
 *
 * Os SSLContext devolvidos servem para depois obter:
 *  - SSLServerSocketFactory (lado servidor) e SSLSocketFactory (lado cliente),
 *  via SSLContext.getServerSocketFactory() / getSocketFactory().
 *
 * Decisões e justificações:
 *  - Protocolo: TLSv1.3 — elimina cipher suites vulneráveis (CBC, RC4, MD5)
 *    e reduz o handshake de 2-RTT para 1-RTT em relação a TLS 1.2.
 *  - KeyStore type: JKS — formato Java histórico. Em produção, PKCS12
 *    (RFC 7292) é o standard recomendado e default em Java 9+.
 *  - Passwords em char[] e não String — char[] pode ser explicitamente
 *    limpo da memória após uso (Arrays.fill(pw, '\0')); Strings são
 *    imutáveis e ficam em memória até GC.
 *  - Algoritmos por defeito do JSSE — usamos getDefaultAlgorithm() para
 *    KeyManagerFactory e TrustManagerFactory; o JDK escolhe o algoritmo
 *    apropriado (normalmente SunX509).
 *
 * Em projeto académico, as credenciais (paths e passwords) são constantes
 * no código com valores conhecidos. Em produção, viriam de variáveis de
 * ambiente, um cofre de segredos (HashiCorp Vault, AWS KMS) ou injecção
 * em deploy — nunca versionadas no repositório.
 */
public final class TLSConfig {

    // Defaults — caminhos e passwords padronizados para o projeto.
    // Não devem ser usados em produção (ver Javadoc da classe).
 

    /** Protocolo TLS a usar nos handshakes. */
    public static final String TLS_PROTOCOL = "TLSv1.3";

    /** Tipo de keystore — JKS por compatibilidade com keytool default. */
    public static final String KEYSTORE_TYPE = "JKS";

    /** Caminho default para o keystore do servidor. */
    public static final String DEFAULT_SERVER_KEYSTORE_PATH =
        "src/main/resources/certs/server-keystore.jks";

    /** Caminho default para o truststore do cliente. */
    public static final String DEFAULT_CLIENT_TRUSTSTORE_PATH =
        "src/main/resources/certs/client-truststore.jks";

    /** Password default para ambos os stores ('changeit'). */
    public static final char[] DEFAULT_PASSWORD =
        new char[] {'c', 'h', 'a', 'n', 'g', 'e', 'i', 't'};

    // Utility class — não instanciável.
    private TLSConfig() {
        throw new AssertionError("TLSConfig é uma utility class.");
    }


    // API pública — construção de SSLContext.

    /**
     * Constrói um SSLContext para o lado servidor usando os defaults
     * do projeto (server-keystore.jks com password 'changeit').
     *
     * Atalho conveniente para o caso 99%; usar a sobrecarga completa
     * apenas se for necessário sobrescrever path ou password.
     */
    public static SSLContext buildServerContext()
            throws GeneralSecurityException, IOException {
        return buildServerContext(
            DEFAULT_SERVER_KEYSTORE_PATH,
            DEFAULT_PASSWORD,
            DEFAULT_PASSWORD
        );
    }

    /**
     * Constrói um SSLContext para o lado servidor.
     * O keystore deve conter a chave privada do servidor e o seu
     * certificado X.509 (autoassinado ou emitido por uma CA).
     *
     * @param keystorePath  caminho para server-keystore.jks
     * @param storePassword password do keystore (store)
     * @param keyPassword   password da chave privada dentro do keystore
     * @return SSLContext pronto para getServerSocketFactory()
     */
    public static SSLContext buildServerContext(String keystorePath,
                                                char[] storePassword,
                                                char[] keyPassword)
            throws GeneralSecurityException, IOException {

        KeyStore ks = loadKeyStore(keystorePath, storePassword);

        KeyManagerFactory kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyPassword);

        SSLContext ctx = SSLContext.getInstance(TLS_PROTOCOL);
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /**
     * Constrói um SSLContext para o lado cliente usando os defaults
     * do projeto (client-truststore.jks com password 'changeit').
     */
    public static SSLContext buildClientContext()
            throws GeneralSecurityException, IOException {
        return buildClientContext(
            DEFAULT_CLIENT_TRUSTSTORE_PATH,
            DEFAULT_PASSWORD
        );
    }

    /**
     * Constrói um SSLContext para o lado cliente.
     * O truststore contém os certificados em que o cliente confia.
     * Para que o handshake seja bem-sucedido, o certificado apresentado
     * pelo servidor tem de bater com algum certificado neste truststore.
     *
     * @param truststorePath caminho para client-truststore.jks
     * @param storePassword  password do truststore
     * @return SSLContext pronto para getSocketFactory()
     */
    public static SSLContext buildClientContext(String truststorePath,
                                                char[] storePassword)
            throws GeneralSecurityException, IOException {

        KeyStore ts = loadKeyStore(truststorePath, storePassword);

        TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance(TLS_PROTOCOL);
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }


    // Auxiliar — carregamento de keystore a partir de ficheiro.

    /**
     * Carrega um KeyStore a partir de um ficheiro JKS no disco.
     * Método package-private para permitir testes; uso normal faz-se
     * via buildServerContext/buildClientContext.
     */
    static KeyStore loadKeyStore(String path, char[] password)
            throws GeneralSecurityException, IOException {

        KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password);
        }
        return ks;
    }
}