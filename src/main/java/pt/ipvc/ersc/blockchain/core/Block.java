package pt.ipvc.ersc.blockchain.core;

import pt.ipvc.ersc.blockchain.utils.HashUtil;

public class Block {

    public String data;
    public String previousHash;
    public String hash;
    public long timestamp;
    public int nonce;

    // criar bloco novo
    public Block(String data, String previousHash) {

        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis();

        this.hash = calculateHash();
    }

    /**
    * Cria um bloco com timestamp e nonce fixos.
    * Usado para o bloco génesis: como todos os campos são determinísticos
    * (constantes), o hash resultante é idêntico em qualquer instância da
    * Blockchain — propriedade essencial para que nós independentes
    * concordem no mesmo génesis e possam validar blocos uns dos outros.
    */
    public Block(String data, String previousHash, long timestamp, int nonce) {
        this.data         = data;
        this.previousHash = previousHash;
        this.timestamp    = timestamp;
        this.nonce        = nonce;
        this.hash         = calculateHash(); // determinístico — sem System.currentTimeMillis()
    }

    // reconstruir bloco vindo da rede
    public Block(String data,
                 String previousHash,
                 long timestamp,
                 int nonce,
                 String hash) {

        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.hash = hash;
    }

    // hash depende de tudo
    public String calculateHash() {

        return HashUtil.sha256(
                previousHash +
                timestamp +
                nonce +
                data
        );
    }

    // mineração
    public void mineBlock(int difficulty) {

        String target = "0".repeat(difficulty);

        while (!hash.startsWith(target)) {
            nonce++;
            hash = calculateHash();
        }

        System.out.println("Bloco minerado: " + hash);
    }

    // converter para string para enviar pela rede
    public String toNetworkString() {

        return data + "|" +
               previousHash + "|" +
               timestamp + "|" +
               nonce + "|" +
               hash;
    }
}