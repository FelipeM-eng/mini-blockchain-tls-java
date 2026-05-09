package pt.ipvc.ersc.blockchain.core;

import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Versão corrigida da Blockchain.java — responsabilidade do Aluno 2.
 *
 * Alterações em relação à versão anterior:
 *
 *  1. 'chain' e 'difficulty' voltam a ser PRIVATE.
 *     Motivo: campos public permitem que qualquer classe do projeto
 *     chame blockchain.chain.add(blocoFalso) sem passar por nenhuma
 *     validação. 'final' numa lista não impede mutação da lista — apenas
 *     impede reatribuição da referência.
 *     Compatibilidade: getDifficulty() e size() expõem o que o Node
 *     precisa. O Samuel precisa de substituir:
 *       blockchain.chain.size()  →  blockchain.size()
 *       blockchain.difficulty    →  blockchain.getDifficulty()
 *
 *  2. addReceivedBlock() agora aplica as 3 regras de consenso + synchronized.
 *     Motivo: a versão anterior verificava apenas previousHash, o que
 *     permitia inserir blocos com hash inválido ou sem Proof-of-Work.
 *
 *  3. isChainValid() agora verifica também o Proof-of-Work (hashPrefix).
 *     Motivo: sem esta verificação, um bloco minerado com dificuldade 0
 *     passaria como válido numa cadeia com dificuldade 2.
 *
 *  4. isValidNewBlock() centraliza as 3 regras (DRY).
 *     Usado por addBlock(), addReceivedBlock() e isChainValid() —
 *     uma única implementação garante consistência entre os três.
 *
 * O que NÃO mudou: construtores, createGenesisBlock(), getLatestBlock(),
 * addBlock(String), getChain(), size().
 * Compatibilidade com Block.java do Diogo: mantida — usa block.hash e
 * block.previousHash directamente (campos públicos existentes).
 *
 * @author Aluno 2 — Luiz Felipe Mesquita
 * @version 3.0
 */
public final class Blockchain {

    private static final String GENESIS_PREVIOUS_HASH = "0";
    private static final String GENESIS_DATA          = "Genesis Block";

    // -----------------------------------------------------------------
    // CAMPOS — todos private (CORRECÇÃO 1)
    //
    // CopyOnWriteArrayList: thread-safe para leituras concorrentes.
    // Escritas são serializadas pelo synchronized nos métodos add*.
    // -----------------------------------------------------------------
    private final List<Block> chain;
    private final int         difficulty;

    // Pré-calculado no construtor — evita "0".repeat() em cada validação.
    private final String hashPrefix;

    // -----------------------------------------------------------------
    // CONSTRUTORES
    // -----------------------------------------------------------------

    /**
     * Construtor padrão usado pelo Node.java do Samuel.
     * Dificuldade 2 → hashes começam com "00".
     */
    public Blockchain() {
        this(2);
    }

    /**
     * Construtor principal.
     *
     * @param difficulty Zeros iniciais exigidos no hash (>= 1).
     * @throws IllegalArgumentException se difficulty < 1.
     */
    public Blockchain(int difficulty) {
        if (difficulty < 1) {
            throw new IllegalArgumentException(
                "Dificuldade deve ser >= 1. Recebido: " + difficulty
            );
        }
        this.difficulty = difficulty;
        this.hashPrefix = "0".repeat(difficulty);
        this.chain      = new CopyOnWriteArrayList<>();

        // A cadeia nunca fica vazia após construção.
        this.chain.add(createGenesisBlock());
    }

    // -----------------------------------------------------------------
    // BLOCO GÉNESIS
    // -----------------------------------------------------------------

    /**
     * Cria o bloco inicial da cadeia.
     * Usa o construtor Block(String data, String previousHash) do Diogo.
     * previousHash = "0" por convenção — não existe bloco anterior.
     */
    private Block createGenesisBlock() {
        Block genesis = new Block(GENESIS_DATA, GENESIS_PREVIOUS_HASH);
        genesis.mineBlock(this.difficulty);
        return genesis;
    }

    // -----------------------------------------------------------------
    // CONSULTA
    // -----------------------------------------------------------------

    /**
     * Devolve o último bloco da cadeia.
     * Invariante: chain.size() >= 1 (garantido pelo construtor).
     */
    public Block getLatestBlock() {
        return this.chain.get(this.chain.size() - 1);
    }

    // -----------------------------------------------------------------
    // ADIÇÃO — mineração local
    // -----------------------------------------------------------------

    /**
     * Constrói, minera e adiciona um bloco com dados locais.
     *
     * A Blockchain constrói o bloco internamente — o chamador nunca
     * recebe um bloco não minerado, eliminando estados inconsistentes.
     *
     * @param data Dados do bloco (não nulos, não em branco).
     * @throws IllegalArgumentException se data for inválido.
     * @throws InvalidBlockException    se a validação de consenso falhar.
     */
    public synchronized void addBlock(String data) throws InvalidBlockException {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Dados do bloco inválidos.");
        }

        Block previousBlock = getLatestBlock();
        Block newBlock      = new Block(data, previousBlock.hash);

        newBlock.mineBlock(this.difficulty);

        // Validação de consenso após mineração.
        // Se falhar, indica bug no mineBlock() do Diogo.
        if (!isValidNewBlock(newBlock, previousBlock)) {
            throw new InvalidBlockException(
                "Bloco recém-minerado falhou na validação de consenso. " +
                "Verifique mineBlock() em Block.java."
            );
        }

        this.chain.add(newBlock);
    }

    // -----------------------------------------------------------------
    // ADIÇÃO — bloco recebido pela rede (CORRECÇÃO 2)
    // -----------------------------------------------------------------

    /**
     * Valida e adiciona um bloco recebido da rede via Node.java.
     *
     * CORRECÇÃO: a versão anterior verificava apenas previousHash.
     * Esta versão aplica as mesmas 3 regras de consenso que addBlock():
     *   1. Encadeamento  — previousHash correcto
     *   2. Integridade   — hash armazenado == hash recalculado
     *   3. Proof-of-Work — hash começa com hashPrefix
     *
     * Sem a verificação 2 e 3, qualquer nó poderia injectar blocos
     * com dados adulterados ou sem mineração válida.
     *
     * synchronized: evita race condition com addBlock() em ambiente
     * multi-thread (ServerNode a receber + Node a minerar em paralelo).
     *
     * @param block Bloco reconstruído pelo parseBlock() do Samuel.
     * @return true se aceite; false se rejeitado (o Node imprime o log).
     */
    public synchronized boolean addReceivedBlock(Block block) {
        if (block == null) {
            System.out.println("[Blockchain] Bloco nulo rejeitado.");
            return false;
        }

        Block previousBlock = getLatestBlock();

        if (!isValidNewBlock(block, previousBlock)) {
            System.out.println("[Blockchain] Bloco rejeitado: falhou na validação de consenso.");
            return false;
        }

        return this.chain.add(block);
    }

    // -----------------------------------------------------------------
    // VALIDAÇÃO DE CONSENSO — lógica centralizada (CORRECÇÃO 4)
    //
    // Três regras obrigatórias para qualquer bloco entrar na cadeia.
    // Método privado reutilizado por addBlock(), addReceivedBlock()
    // e isChainValid() — garante que todos aplicam as mesmas regras.
    // -----------------------------------------------------------------

    /**
     * Verifica se newBlock pode ser inserido a seguir a previousBlock.
     *
     * Usa block.hash e block.previousHash directamente — campos públicos
     * do Block.java do Diogo — sem necessidade de getters.
     */
    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {
        Objects.requireNonNull(newBlock,      "newBlock não pode ser nulo.");
        Objects.requireNonNull(previousBlock, "previousBlock não pode ser nulo.");

        // Regra 1 — Encadeamento: o bloco tem de apontar para o anterior correcto.
        if (!newBlock.previousHash.equals(previousBlock.hash)) {
            return false;
        }

        // Regra 2 — Integridade: hash armazenado == hash recalculado a partir
        // dos dados actuais. Detecta adulteração após mineração.
        if (!newBlock.hash.equals(newBlock.calculateHash())) {
            return false;
        }

        // Regra 3 — Proof-of-Work: o hash tem de satisfazer a dificuldade.
        // hashPrefix está pré-calculado no construtor — sem alocações aqui.
        return newBlock.hash.startsWith(this.hashPrefix);
    }

    // -----------------------------------------------------------------
    // VALIDAÇÃO DA CADEIA COMPLETA (CORRECÇÃO 3)
    // -----------------------------------------------------------------

    /**
     * Verifica a integridade de toda a cadeia.
     *
     * CORRECÇÃO: a versão anterior não verificava Proof-of-Work.
     * Agora reutiliza isValidNewBlock() — as 3 regras são aplicadas
     * a cada par de blocos consecutivos.
     *
     * @return true se a cadeia estiver íntegra.
     * @throws InvalidChainException se a cadeia estiver vazia.
     */
    public boolean isChainValid() throws InvalidChainException {
        if (this.chain.isEmpty()) {
            throw new InvalidChainException("Cadeia vazia — estado corrompido.");
        }

        // i=1: o génesis não tem predecessor para verificar.
        for (int i = 1; i < this.chain.size(); i++) {
            Block current  = this.chain.get(i);
            Block previous = this.chain.get(i - 1);

            if (!isValidNewBlock(current, previous)) {
                return false;
            }
        }

        return true;
    }

    // -----------------------------------------------------------------
    // GETTERS — substituem os campos públicos removidos
    // -----------------------------------------------------------------

    /**
     * Vista imutável da cadeia.
     * Nenhum externo pode chamar getChain().add() — Collections.unmodifiableList
     * lança UnsupportedOperationException em qualquer tentativa de mutação.
     */
    public List<Block> getChain() {
        return Collections.unmodifiableList(this.chain);
    }

    /**
     * Substitui blockchain.difficulty no Node.java do Samuel.
     * Mesma semântica, sem expor o campo.
     */
    public int getDifficulty() {
        return this.difficulty;
    }

    /**
     * Substitui blockchain.chain.size() no Node.java do Samuel.
     * Mesma semântica, sem expor a lista.
     */
    public int size() {
        return this.chain.size();
    }
}