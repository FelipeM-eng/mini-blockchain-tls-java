package pt.ipvc.ersc.blockchain.core;

import java.util.ArrayList;

public class Blockchain {

    public ArrayList<Block> chain;
    public int difficulty = 4;

    public Blockchain() {
        chain = new ArrayList<>();
        chain.add(createGenesisBlock());
    }

    // primeiro bloco (não tem anterior)
    private Block createGenesisBlock() {
        Block genesis = new Block("Genesis Block", "0");
        genesis.mineBlock(difficulty);
        return genesis;
    }

    // último bloco da cadeia
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    // adiciona novo bloco (já ligado ao anterior)
    public void addBlock(String data) {
        Block newBlock = new Block(data, getLatestBlock().hash);
        newBlock.mineBlock(difficulty);
        chain.add(newBlock);
    }

    // verifica se alguém mexeu na cadeia
    public boolean isChainValid() {

        for (int i = 1; i < chain.size(); i++) {

            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            // hash tem de bater certo com o conteúdo
            if (!current.hash.equals(current.calculateHash()))
                return false;

            // ligação entre blocos tem de estar correta
            if (!current.previousHash.equals(previous.hash))
                return false;
        }

        return true;
    }
}