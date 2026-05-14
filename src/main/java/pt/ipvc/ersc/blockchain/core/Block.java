package pt.ipvc.ersc.blockchain.core;

import pt.ipvc.ersc.blockchain.utils.HashUtil;

/**
 * Block — unidade fundamental da blockchain.
 *
 * Imutabilidade:
 *  - data, previousHash e timestamp são final: definidos no construtor e nunca
 *    mais mudam. Garantem que o conteúdo histórico de um bloco aceite na cadeia
 *    não pode ser alterado sem detecção (a alteração quebra a regra de
 *    integridade em Blockchain.isValidNewBlock).
 *  - nonce e hash mudam apenas durante mineBlock(); após a mineração ficam
 *    estáveis. Não são final apenas porque a mineração precisa de os ajustar
 *    iterativamente.
 *
 * Todos os campos são private. O acesso de leitura faz-se por getters; não há
 * setters públicos. Isto impede que outras classes mutilem um bloco já minerado
 * — propriedade essencial para a noção de "registo imutável" em blockchain.
 *
 * Responsável: Samuel Ferreira (33846).
 */
public class Block {

    private final String data;
    private final String previousHash;
    private final long timestamp;

    private int nonce;
    private String hash;

    /**
     * Cria um bloco novo a partir de dados locais.
     * O timestamp é capturado no momento da criação; o nonce começa a 0 e o
     * hash inicial é calculado imediatamente (será depois refinado por
     * mineBlock).
     */
    public Block(String data, String previousHash) {
        this.data         = data;
        this.previousHash = previousHash;
        this.timestamp    = System.currentTimeMillis();
        this.nonce        = 0;
        this.hash         = calculateHash();
    }

    /**
     * Reconstrói um bloco a partir de campos recebidos da rede.
     * Não recalcula nem revalida nada — essa responsabilidade pertence à
     * Blockchain (isValidNewBlock). Aqui apenas se reconstroi o objecto tal
     * como foi serializado pelo emissor.
     */
    public Block(String data,
                 String previousHash,
                 long timestamp,
                 int nonce,
                 String hash) {
        this.data         = data;
        this.previousHash = previousHash;
        this.timestamp    = timestamp;
        this.nonce        = nonce;
        this.hash         = hash;
    }

    /**
     * Calcula o SHA-256 dos campos que definem a identidade do bloco.
     * Não escreve em this.hash — devolve o valor para quem chama decidir o
     * que fazer (mineBlock atualiza; isValidNewBlock compara com o hash
     * armazenado para detectar adulteração).
     */
    public String calculateHash() {
        return HashUtil.sha256(
                previousHash +
                timestamp +
                nonce +
                data
        );
    }

    /**
     * Proof-of-Work: incrementa nonce até o hash começar com 'difficulty' zeros.
     * Operação determinística dado (data, previousHash, timestamp) — a partir
     * destes três, o nonce/hash resultantes são os primeiros que satisfazem
     * a dificuldade.
     */
    public void mineBlock(int difficulty) {
        final String target = "0".repeat(difficulty);

        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }

        System.out.println("Bloco minerado: " + hash);
    }

    /**
     * Serialização para envio em rede.
     * Formato pipe-delimited com 5 campos por esta ordem:
     *   data | previousHash | timestamp | nonce | hash
     *
     * Limitação conhecida: se 'data' contiver o caracter '|', a desserialização
     * parte-se. Resolvido em commit posterior (codificação Base64 do data).
     */
    public String toNetworkString() {
        return data + "|" +
               previousHash + "|" +
               timestamp + "|" +
               nonce + "|" +
               hash;
    }

    // ------------------------------------------------------------------
    // Getters (acesso de leitura — não há setters por desígnio)
    // ------------------------------------------------------------------

    public String getData() {
        return data;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }
}