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
 * Responsabilidades:
 * - Testar se os contextos TLS carregam corretamente.
 * - Testar se dois nós trocam blocos corretamente via TLS.
 * - Testar se um bloco fora da cadeia é rejeitado localmente após sincronização.
 */
public class NetworkTlsIntegrationTest {

    // define as portas e o host para os nós.
    private static final int PORT_A = 25100;
    private static final int PORT_B = 25101;
    private static final String HOST = "localhost";

    //**
    // Testa se os certificados e o projeto estão corretos.
    //**
    @BeforeClass
    public static void requireCertsAndProjectRoot() {   
        // verifica se os certificados existem.
        Path serverKs = Path.of(TLSConfig.DEFAULT_SERVER_KEYSTORE_PATH);
        // verifica se o truststore existe.
        Path clientTs = Path.of(TLSConfig.DEFAULT_CLIENT_TRUSTSTORE_PATH);
        // se os certificados não existem, então lança uma exceção.
        if (!serverKs.toFile().exists() || !clientTs.toFile().exists()) {
            // lança uma exceção com a mensagem "Certificados não encontrados. Execute mvn test na raiz do projeto.".
            throw new IllegalStateException(
                    "Certificados não encontrados. Execute mvn test na raiz do projeto."
            );
        }
    }

    //**
    // Testa se os contextos TLS carregam corretamente.
    //**
    @Test
    public void tlsContextsLoadSuccessfully() throws Exception {
        // cria um contexto TLS para o servidor.
        SSLContext server = TLSConfig.buildServerContext();
        // cria um contexto TLS para o cliente.
        SSLContext client = TLSConfig.buildClientContext();
        // verifica se o protocolo TLS é o mesmo para o servidor e o cliente.
        assertEquals(TLSConfig.TLS_PROTOCOL, server.getProtocol());
        // verifica se o protocolo TLS é o mesmo para o servidor e o cliente.
        assertEquals(TLSConfig.TLS_PROTOCOL, client.getProtocol());
    }

    //**
    // Testa se dois nós trocam blocos corretamente via TLS.
    //**
    @Test
    public void twoNodesExchangeBlocksOverTls()
            throws Exception {

        // cria um novo nó A.
        Node nodeA = new Node(PORT_A);
        // cria um novo nó B.
        Node nodeB = new Node(PORT_B);
        // inicia o servidor TLS para o nó A.
        nodeA.startServer();
        // inicia o servidor TLS para o nó B.
        nodeB.startServer();

        // espera 2500 milissegundos.
        Thread.sleep(2500);

        // minera um bloco com a mensagem "integração A -> B".
        Block fromA = nodeA.mineBlock("integração A -> B");
        // envia o bloco para o nó B.
        nodeA.sendBlock(fromA, HOST, PORT_B);
        Thread.sleep(2000);

        // verifica se a cadeia do nó B tem 2 blocos.
        assertEquals(2, nodeB.getBlockchain().size());
        // verifica se a cadeia do nó B é válida.
        assertTrue(nodeB.getBlockchain().isChainValid());

        // minera um bloco com a mensagem "integração B -> A".
        Block fromB = nodeB.mineBlock("integração B -> A");
        // envia o bloco para o nó A.
        nodeB.sendBlock(fromB, HOST, PORT_A);
        Thread.sleep(2000);

        // verifica se a cadeia do nó A tem 3 blocos.
        assertEquals(3, nodeA.getBlockchain().size());
        // verifica se a cadeia do nó B tem 3 blocos.
        assertEquals(3, nodeB.getBlockchain().size());
        // verifica se a cadeia do nó A é válida.
        assertTrue(nodeA.getBlockchain().isChainValid());
        // verifica se a cadeia do nó B é válida.
        assertTrue(nodeB.getBlockchain().isChainValid());
    }

    //**
    // Testa se um bloco fora da cadeia é rejeitado localmente após sincronização.
    //**
    @Test
    public void orphanBlockRejectedLocallyAfterSync() throws Exception {

        // define a porta do nó A.
        int portA = PORT_A + 10;
        // define a porta do nó B.
        int portB = PORT_B + 10;

        // cria um novo nó A.
        Node nodeA = new Node(portA);
        // cria um novo nó B.
        Node nodeB = new Node(portB);

        // inicia o servidor TLS para o nó A.
        nodeA.startServer();
        // inicia o servidor TLS para o nó B.
        nodeB.startServer();
        // espera 3000 milissegundos.
        Thread.sleep(3000);

        // minera um bloco com a mensagem "bloco válido em A".
        Block valid = nodeA.mineBlock("bloco válido em A");
        // envia o bloco para o nó B.
        nodeA.sendBlock(valid, HOST, portB);
        // espera 2500 milissegundos.
        Thread.sleep(2500);

        // verifica se a cadeia do nó B tem 2 blocos.
        assertEquals(2, nodeB.getBlockchain().size());

        // cria um bloco fora da cadeia com a mensagem "órfão" e o hash "previous_invalido".
        Block orphan = new Block("órfão", "previous_invalido");
        // minera o bloco.
        orphan.mineBlock(nodeB.getBlockchain().getDifficulty());
        // verifica se o bloco foi rejeitado.
        assertFalse(nodeB.getBlockchain().addReceivedBlock(orphan));
        // verifica se a cadeia do nó B tem 2 blocos.
        assertEquals(2, nodeB.getBlockchain().size());
    }
}
