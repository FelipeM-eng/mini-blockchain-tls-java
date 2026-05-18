package pt.ipvc.ersc.blockchain.core;

import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
    Classe Blockchain — gestão da cadeia de blocos.
    Responsabilidades:
    - Manter a lista de blocos (chain).
    - Gerar o bloco génesis no construtor.
    - Adicionar blocos minerados localmente (addBlock).
    - Validar e adicionar blocos recebidos da rede (addReceivedBlock).
    - Validar a integridade da cadeia completa (isChainValid).
    - Fornecer acesso controlado à cadeia e à dificuldade (getChain, getDifficulty).
 */
public final class Blockchain {

    // Dado que o bloco génesis é um caso especial, então definimos constantes para seus valores de Hash e dados.
    private static final String GENESIS_PREVIOUS_HASH = "0";
    private static final String GENESIS_DATA          = "Genesis Block";

    // Valores fixos para que o bloco génesis seja determinístico — idêntico
    // em todos os nós, independentemente de quando arrancam. Sem isto, cada
    // nó teria um génesis com timestamp/hash diferente e não conseguiriam
    // sincronizar a partir do bloco 1.
    private static final long   GENESIS_TIMESTAMP     = 0L;
    private static final long    GENESIS_NONCE        = 0L;

    // E definimos o encadeamento da cadeia como uma lista de blocos.
    private final List<Block> chain;

    // a dificuldade é um inteiro que define quantos zeros iniciais o hash deve ter para ser considerado válido.
    private final int         difficulty;

    // Pré-calculado no construtor — evita "0".repeat() em cada validação.
    private final String hashPrefix;


    /**
     Construtor padrão usado pelo Node.java
     chama o contrutor principal com dificuldade = 2
     */
    public Blockchain() {
        this(2);
    }

    /**
    Construtor principal.
    Permite configurar a dificuldade da mineração.
    calcula hashPrefix = "0".repeat(difficulty) uma vez — otimização para validação de blocos.
    cria o bloco génesis e o adiciona à cadeia — garante que a cadeia nunca fica vazia.
     */
    public Blockchain(int difficulty) {
        //dado que se verifica que a dificuldade é um valor inteiro positivo, então lançamos uma exceção caso seja menor que 1.
        if (difficulty < 1) {
            throw new IllegalArgumentException(
                "Dificuldade deve ser >= 1. Recebido: " + difficulty
            );
        }

        //quando definimos os campos, então atribuímos os valores recebidos e calculados.
        this.difficulty = difficulty;
        this.hashPrefix = "0".repeat(difficulty);
        this.chain      = new CopyOnWriteArrayList<>();

        // então chama o metodo createGenesisBlock() para criar o bloco génesis e adicioná-lo à cadeia.
        this.chain.add(createGenesisBlock());
    }

   
    /**
    Cria o bloco inicial da cadeia.
    Génesis com timestamp/nonce fixos — hash é totalmente determinístico,
    logo igual em todos os nós. Não é minerado: o génesis é um caso
    especial que não precisa de satisfazer o PoW (a regra de PoW só se
    aplica a blocos validados em isValidNewBlock, e o génesis é
    excluído desse caminho — ver loop em isChainValid que começa em i=1).
     */
    private Block createGenesisBlock() {
        return new Block(
            GENESIS_DATA,
            GENESIS_PREVIOUS_HASH,
            GENESIS_TIMESTAMP,
            GENESIS_NONCE
        );
    }

    /**
     Devolve o último bloco da cadeia.
     Invariante: chain.size() >= 1 (garantido pelo construtor).
     */
    public Block getLatestBlock() {
        return this.chain.get(this.chain.size() - 1);
    }

    
    /**
     Constrói, minera e adiciona um bloco com dados locais.
     A Blockchain constrói o bloco internamente — o chamador nunca
     recebe um bloco não minerado, eliminando estados inconsistentes.
     */
    public synchronized Block addBlock(String data) throws InvalidBlockException {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Dados do bloco inválidos.");
        }

        Block previousBlock = getLatestBlock();
        Block newBlock      = new Block(data, previousBlock.getHash());

        newBlock.mineBlock(this.difficulty);

        if (!isValidNewBlock(newBlock, previousBlock)) {
            throw new InvalidBlockException(
                "Bloco recém-minerado falhou na validação de consenso. " +
                "Verifique mineBlock() em Block.java."
            );
        }

        this.chain.add(newBlock);
        return newBlock;
    }

   
    /**
     Valida e adiciona um bloco recebido da rede via Node.java.
        Regras de consenso:
        1. Encadeamento: block.previousHash deve ser igual ao hash do último bloco da cadeia.
        2. Integridade: block.hash deve ser igual ao hash recalculado a partir dos dados atuais do bloco.
        3. Proof-of-Work: block.hash deve satisfazer a dificuldade (começar com hashPrefix).
     Retorna true se o bloco for válido e adicionado com sucesso, ou false se for inválido
     */
    public synchronized boolean addReceivedBlock(Block block) {
        // Dado que o bloco recebido não pode ser nulo, então rejeitamos blocos nulos imediatamente.
        if (block == null) {
            System.out.println("[Blockchain] Bloco nulo rejeitado.");
            return false;
        }

        // e obtemos o último bloco da cadeia para validar o encadeamento do bloco recebido.
        Block previousBlock = getLatestBlock();

        // quando validamos o bloco recebido usando as regras de consenso definidas em isValidNewBlock().
        if (!isValidNewBlock(block, previousBlock)) {
            System.out.println("[Blockchain] Bloco rejeitado: falhou na validação de consenso.");
            return false;
        }
        
        // então adicionamos o bloco válido à cadeia e retornamos true para indicar sucesso, caso passe no isValidNewBlock().
        return this.chain.add(block);
    }


    /**
        Valida um novo bloco em relação ao bloco anterior usando as regras de consenso:
        1. Encadeamento: newBlock.previousHash deve ser igual ao hash do previousBlock.
        2. Integridade: newBlock.hash deve ser igual ao hash recalculado a partir dos dados atuais de newBlock.
        3. Proof-of-Work: newBlock.hash deve satisfazer a dificuldade (começar com hashPrefix).
        Retorna true se o bloco for válido, ou false se for inválido.
     */
    private boolean isValidNewBlock(Block newBlock, Block previousBlock) {

        // Dado que ambos os blocos não podem ser nulos, então lançamos uma exceção caso algum deles seja nulo.
        Objects.requireNonNull(newBlock,      "newBlock não pode ser nulo.");
        Objects.requireNonNull(previousBlock, "previousBlock não pode ser nulo.");

        // Regra 1 — Encadeamento: o bloco tem de apontar para o anterior correcto.
        if (!newBlock.getPreviousHash().equals(previousBlock.getHash())) {
            return false;
        }

        // Regra 2 — Integridade: hash armazenado == hash recalculado a partir
        // dos dados atuais. Detecta adulteração após mineração.
        if (!newBlock.getHash().equals(newBlock.calculateHash())) {
            return false;
        }

        // Regra 3 — Proof-of-Work: o hash tem de satisfazer a dificuldade.
        // hashPrefix está pré-calculado no construtor — sem alocações aqui.
        return newBlock.getHash().startsWith(this.hashPrefix);
    }


    /**
     Verifica a integridade de toda a cadeia.
        Regras de validação:
        1. A cadeia não pode estar vazia (deve conter pelo menos o bloco génesis).
        2. Cada bloco (exceto o génesis) deve ser válido em relação ao seu predecessor usando isValidNewBlock().
     Retorna true se a cadeia for válida, ou lança InvalidChainException se for inválida.
     */
    public boolean isChainValid() throws InvalidChainException {
        if (this.chain.isEmpty()) {
            throw new InvalidChainException("Cadeia vazia — estado corrompido.");
        }

        // i=1: o génesis não tem predecessor para verificar.
        for (int i = 1; i < this.chain.size(); i++) {

            // Dado que cada bloco deve ser válido em relação ao seu predecessor, então iteramos pela cadeia a partir do segundo bloco (i=1) e validamos cada bloco usando isValidNewBlock().
            Block current  = this.chain.get(i);
            Block previous = this.chain.get(i - 1);

            if (!isValidNewBlock(current, previous)) {
                return false;
            }
        }

        return true;
    }


    /**
     Vista imutável da cadeia.
     Nenhum externo pode chamar getChain().add() — Collections.unmodifiableList
     lança UnsupportedOperationException em qualquer tentativa de mutação.
     */
    public List<Block> getChain() {

        // dado que collections.unmodifiableList() 
        return Collections.unmodifiableList(this.chain);
    }

    /**
        Acesso à dificuldade de mineração configurada.
     */
    public int getDifficulty() {
        return this.difficulty;
    }

    /**
        Retorna o número de blocos na cadeia.
     */
    public int size() {
        return this.chain.size();
    }
}