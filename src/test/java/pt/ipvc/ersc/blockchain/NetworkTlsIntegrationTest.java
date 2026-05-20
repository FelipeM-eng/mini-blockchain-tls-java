package pt.ipvc.ersc.blockchain;

import org.junit.BeforeClass;
import org.junit.Test;
import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.network.Node;
import pt.ipvc.ersc.blockchain.security.TLSConfig;

import javax.net.ssl.SSLContext;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testes de integração TLS + propagação de blocos entre dois nós (localhost).
 *
 * Executar a partir da raiz do projeto: mvn test
 * (os caminhos dos .jks em TLSConfig são relativos à raiz).
 */
public class NetworkTlsIntegrationTest {

    private static final int PORT_A = 25100;
    private static final int PORT_B = 25101;
    private static final String HOST = "localhost";

    @BeforeClass
    public static void requireCertsAndProjectRoot() {
        Path serverKs = Path.of(TLSConfig.DEFAULT_SERVER_KEYSTORE_PATH);
        Path clientTs = Path.of(TLSConfig.DEFAULT_CLIENT_TRUSTSTORE_PATH);
        if (!serverKs.toFile().exists() || !clientTs.toFile().exists()) {
            throw new IllegalStateException(
                    "Certificados não encontrados. Execute mvn test na raiz do projeto."
            );
        }
    }

    @Test
    public void tlsContextsLoadSuccessfully() throws Exception {
        SSLContext server = TLSConfig.buildServerContext();
        SSLContext client = TLSConfig.buildClientContext();
        assertEquals(TLSConfig.TLS_PROTOCOL, server.getProtocol());
        assertEquals(TLSConfig.TLS_PROTOCOL, client.getProtocol());
    }

    @Test
    public void twoNodesExchangeBlocksOverTls()
            throws Exception {

        Node nodeA = new Node(PORT_A);
        Node nodeB = new Node(PORT_B);
        nodeA.startServer();
        nodeB.startServer();

        Thread.sleep(2500);

        Block fromA = nodeA.mineBlock("integração A -> B");
        nodeA.sendBlock(fromA, HOST, PORT_B);
        Thread.sleep(2000);

        assertEquals(2, nodeB.getBlockchain().size());
        assertTrue(nodeB.getBlockchain().isChainValid());

        Block fromB = nodeB.mineBlock("integração B -> A");
        nodeB.sendBlock(fromB, HOST, PORT_A);
        Thread.sleep(2000);

        assertEquals(3, nodeA.getBlockchain().size());
        assertEquals(3, nodeB.getBlockchain().size());
        assertTrue(nodeA.getBlockchain().isChainValid());
        assertTrue(nodeB.getBlockchain().isChainValid());
    }

    @Test
    public void orphanBlockRejectedLocallyAfterSync() throws Exception {

        int portA = PORT_A + 10;
        int portB = PORT_B + 10;

        Node nodeA = new Node(portA);
        Node nodeB = new Node(portB);

        nodeA.startServer();
        nodeB.startServer();
        Thread.sleep(3000);

        Block valid = nodeA.mineBlock("bloco válido em A");
        nodeA.sendBlock(valid, HOST, portB);
        Thread.sleep(2500);

        assertEquals(2, nodeB.getBlockchain().size());

        Block orphan = new Block("órfão", "previous_invalido");
        orphan.mineBlock(nodeB.getBlockchain().getDifficulty());
        assertFalse(nodeB.getBlockchain().addReceivedBlock(orphan));
        assertEquals(2, nodeB.getBlockchain().size());
    }
}
