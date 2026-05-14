package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.core.Blockchain;
import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;

/**
 * MainTest — testes ao núcleo Blockchain/Block em memória (sem rede).
 *
 * Verifica, isoladamente, as garantias essenciais do projeto:
 *  - existência e estado do bloco génesis
 *  - Proof-of-Work satisfaz a dificuldade configurada
 *  - adição e validação de blocos minerados localmente
 *  - rejeição de blocos com encadeamento inválido
 *  - validação dos parâmetros de entrada
 *
 * Responsável: Samuel Ferreira (33846) — Semana 1.
 */
public class MainTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[OK]   " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=== MainTest: núcleo Blockchain ===\n");

        testGenesisBlock();
        testProofOfWork();
        testAddAndValidateBlocks();
        testRejectInvalidPreviousHash();
        testInvalidDifficultyIsRejected();

        System.out.println("\n=== Resumo: " + passed + " OK, " + failed + " FAIL ===");

        if (failed > 0) {
            System.exit(1);
        }
    }

    /** O construtor deve criar a cadeia com um único bloco — o génesis. */
    private static void testGenesisBlock() {
        Blockchain bc = new Blockchain();
        check("Cadeia inicia com génesis (size=1)", bc.size() == 1);
        check("Génesis tem previousHash='0'",
              "0".equals(bc.getLatestBlock().previousHash));
    }

    /** O hash de um bloco minerado deve começar com 'difficulty' zeros. */
    private static void testProofOfWork() {
        int difficulty = 3;
        Blockchain bc = new Blockchain(difficulty);
        String prefix = "0".repeat(difficulty);
        check("Hash do génesis satisfaz PoW (difficulty=" + difficulty + ")",
              bc.getLatestBlock().hash.startsWith(prefix));
    }

    /** Blocos minerados localmente são adicionados e a cadeia continua válida. */
    private static void testAddAndValidateBlocks()
            throws InvalidBlockException, InvalidChainException {
        Blockchain bc = new Blockchain(2);
        bc.addBlock("tx1");
        bc.addBlock("tx2");
        check("3 blocos na cadeia (génesis + 2)", bc.size() == 3);
        check("Cadeia válida após adições", bc.isChainValid());
    }

    /** Um bloco que não aponta para o último hash da cadeia deve ser rejeitado. */
    private static void testRejectInvalidPreviousHash() {
        Blockchain bc = new Blockchain(2);

        // Bloco órfão: previousHash não corresponde a nenhum bloco da cadeia.
        // Mesmo minerado correctamente, deve falhar a regra de encadeamento.
        Block orphan = new Block("bloco fora da cadeia", "previousHash_invalido");
        orphan.mineBlock(bc.getDifficulty());

        boolean accepted = bc.addReceivedBlock(orphan);
        check("Bloco com previousHash inválido é rejeitado", !accepted);
        check("Cadeia mantém tamanho original (size=1)", bc.size() == 1);
    }

    /** Dificuldade < 1 não faz sentido e deve lançar exceção. */
    private static void testInvalidDifficultyIsRejected() {
        try {
            new Blockchain(0);
            check("Difficulty=0 deve lançar IllegalArgumentException", false);
        } catch (IllegalArgumentException e) {
            check("Difficulty=0 lança IllegalArgumentException", true);
        }
    }
}