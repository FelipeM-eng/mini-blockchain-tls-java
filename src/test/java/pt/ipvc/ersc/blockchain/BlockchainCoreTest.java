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
 */
public class BlockchainCoreTest {

    @Test
    public void genesisBlockIsCreatedOnStartup() {
        Blockchain bc = new Blockchain();
        assertEquals(1, bc.size());
        assertEquals("0", bc.getLatestBlock().getPreviousHash());
    }

    @Test
    public void minedBlockSatisfiesProofOfWork() throws Exception {
        int difficulty = 3;
        Blockchain bc = new Blockchain(difficulty);
        bc.addBlock("bloco PoW");
        String prefix = "0".repeat(difficulty);
        assertTrue(bc.getLatestBlock().getHash().startsWith(prefix));
    }

    @Test
    public void chainRemainsValidAfterAddingBlocks()
            throws InvalidBlockException, InvalidChainException {
        Blockchain bc = new Blockchain(2);
        bc.addBlock("tx1");
        bc.addBlock("tx2");
        assertEquals(3, bc.size());
        assertTrue(bc.isChainValid());
    }

    @Test
    public void invalidPreviousHashIsRejected() {
        Blockchain bc = new Blockchain(2);
        Block orphan = new Block("fora da cadeia", "hash_invalido");
        orphan.mineBlock(bc.getDifficulty());
        assertFalse(bc.addReceivedBlock(orphan));
        assertEquals(1, bc.size());
    }

    @Test
    public void difficultyBelowOneThrows() {
        try {
            new Blockchain(0);
            fail("Esperava IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
