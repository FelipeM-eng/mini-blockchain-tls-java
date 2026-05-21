package pt.ipvc.ersc.blockchain;

import org.junit.Test;
import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.core.Blockchain;
import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testes de integração do núcleo blockchain (sem rede).
 * Responsabilidades:
 * - Testar a criação do bloco genesis.
 * - Testar se o bloco minerado satisfaz a prova de trabalho.
 * - Testar se a cadeia permanece válida após adicionar blocos.
 * - Testar se um bloco fora da cadeia é rejeitado.
 * - Testar se uma dificuldade abaixo de 1 lança uma exceção.
 */
public class BlockchainCoreTest {

    //**
    // Testa se o bloco genesis é criado na inicialização.
    //**
    @Test
    public void genesisBlockIsCreatedOnStartup() {
        // cria uma nova cadeia com a dificuldade padrão 2.
        Blockchain bc = new Blockchain();
        // verifica se a cadeia tem 1 bloco.
        assertEquals(1, bc.size());
        // verifica se o hash do bloco anterior é 0.
        assertEquals("0", bc.getLatestBlock().getPreviousHash());
    }

    //**
    // Testa se o bloco minerado satisfaz a prova de trabalho.
    //**
    @Test
    public void minedBlockSatisfiesProofOfWork() throws Exception {
        // define a dificuldade como 3.
        int difficulty = 3;
        // cria uma nova cadeia com a dificuldade 3.
        Blockchain bc = new Blockchain(difficulty);
        // adiciona um bloco com a mensagem "bloco PoW".
        bc.addBlock("bloco PoW");
        // define o prefixo como "0".repeat(difficulty).
        String prefix = "0".repeat(difficulty);
        // verifica se o hash do bloco minerado começa com o prefixo.
        assertTrue(bc.getLatestBlock().getHash().startsWith(prefix));
    }

    //**
    // Testa se a cadeia permanece válida após adicionar blocos.
    // lança uma exceção se o bloco for inválido ou a cadeia for inválida.
    //**
    @Test
    public void chainRemainsValidAfterAddingBlocks()
            throws InvalidBlockException, InvalidChainException {
        // cria uma nova cadeia com a dificuldade 2.
        Blockchain bc = new Blockchain(2);
        // adiciona um bloco com a mensagem "tx1".
        bc.addBlock("tx1");
        // adiciona um bloco com a mensagem "tx2".
        bc.addBlock("tx2");
        // verifica se a cadeia tem 3 blocos.
        assertEquals(3, bc.size());
        // verifica se a cadeia é válida.
        assertTrue(bc.isChainValid());
    }

    //**
    // Testa se um bloco fora da cadeia é rejeitado.
    // lança uma exceção se o bloco for inválido ou a cadeia for inválida.
    //**
    @Test
    public void invalidPreviousHashIsRejected() {
        // cria uma nova cadeia com a dificuldade 2.
        Blockchain bc = new Blockchain(2);
        // cria um bloco fora da cadeia com a mensagem "fora da cadeia" e o hash "hash_invalido".
        Block orphan = new Block("fora da cadeia", "hash_invalido");
        // minera o bloco.
        orphan.mineBlock(bc.getDifficulty());
        // verifica se o bloco foi rejeitado.
        assertFalse(bc.addReceivedBlock(orphan));
        // verifica se a cadeia tem 1 bloco.
        assertEquals(1, bc.size());
    }

    //**
    // Testa se uma dificuldade abaixo de 1 lança uma exceção.
    //**
    @Test
    public void difficultyBelowOneThrows() {
        try {
            // cria uma nova cadeia com a dificuldade 0.
            new Blockchain(0);
            fail("Esperava IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // verifica se a exceção é lançada.
            assertTrue(expected.getMessage().contains("Dificuldade deve ser >= 1. Recebido: 0"));
        }
    }
}
