package pt.ipvc.ersc.blockchain.core;

import java.util.ArrayList;

public class Blockchain {

    public ArrayList<Block> chain;

    // dificuldade da mineração
    public int difficulty = 4;

    public Blockchain() {

        chain = new ArrayList<>();

        chain.add(createGenesisBlock());
    }

    // genesis block fixo para todos os nós
    // todos os nós da rede têm de começar iguais
    private Block createGenesisBlock() {

        return new Block(
                "Genesis Block",
                "0",
                1752449698721L,
                88484,
                "0000d8f0d8b698d6d2b7f2b31db5f7c91ed746a6d2d3fbb6f9c4e6d8b1a2c3d4"
        );
    }

    // último bloco da blockchain
    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    // adicionar bloco criado localmente
    public void addBlock(String data) {

        Block newBlock = new Block(
                data,
                getLatestBlock().hash
        );

        newBlock.mineBlock(difficulty);

        chain.add(newBlock);
    }

    // adicionar bloco recebido pela rede
    public boolean addReceivedBlock(Block block) {

        // verificar integridade
        if (!block.hash.equals(block.calculateHash())) {
            System.out.println("Hash inválida");
            return false;
        }

        // verificar PoW
        if (!block.hash.startsWith("0".repeat(difficulty))) {
            System.out.println("PoW inválido");
            return false;
        }

        // verificar ligação à blockchain
        if (!block.previousHash.equals(getLatestBlock().hash)) {
            System.out.println("Bloco não liga à blockchain");
            return false;
        }

        chain.add(block);

        return true;
    }

    // validar blockchain inteira
    public boolean isChainValid() {

        for (int i = 1; i < chain.size(); i++) {

            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            // hash alterada
            if (!current.hash.equals(current.calculateHash()))
                return false;

            // ligação quebrada
            if (!current.previousHash.equals(previous.hash))
                return false;

            // PoW inválido
            if (!current.hash.startsWith("0".repeat(difficulty)))
                return false;
        }

        return true;
    }
}