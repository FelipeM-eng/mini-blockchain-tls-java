package pt.ipvc.ersc.blockchain.core;

import pt.ipvc.ersc.blockchain.utils.HashUtil;

public class Block {

    public String data; 
    public String previousHash; 
    public String hash;
    public long timestamp; 
    public int nonce;

    public Block(String data, String previousHash) {
        this.data = data;
        this.previousHash = previousHash;
        this.timestamp = System.currentTimeMillis();
        this.hash = calculateHash();
    }

    // calcula o hash com base em todos os dados
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

        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }

        System.out.println("Bloco minerado: " + hash);
    }
}