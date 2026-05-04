package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.core.Blockchain;

public class Main {

    public static void main(String[] args) {

        Blockchain bc = new Blockchain();

        bc.addBlock("Bloco 1");
        bc.addBlock("Bloco 2");
        bc.addBlock("Bloco 3");

        System.out.println("Blockchain válida? " + bc.isChainValid());

        // teste de ataque
        // bc.chain.get(1).data = "HACKED";
        // bc.chain.get(1).nonce = 999;
        // bc.chain.get(2).previousHash = "123";
        // bc.chain.get(1).hash = "0000fakehash";

        System.out.println("Depois de alterar:");
        System.out.println("Blockchain válida? " + bc.isChainValid());
    }
}