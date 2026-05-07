package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.network.Node;

public class Main {

    public static void main(String[] args) throws Exception {

        // criar nós
        Node nodeA = new Node(5000);
        Node nodeB = new Node(5001);

        // começar servidores
        nodeA.startServer();
        nodeB.startServer();

        // dar tempo aos sockets
        Thread.sleep(2000);

        // nodeA minera bloco
        Block block = nodeA.mineBlock("samuel enviou 10 coins");

        // enviar para nodeB
        nodeA.sendBlock(block, "localhost", 5001);

        Thread.sleep(2000);

        // verificar chains
        System.out.println("\nNode A válida? " +
                nodeA.getBlockchain().isChainValid());

        System.out.println("Node B válida? " +
                nodeB.getBlockchain().isChainValid());
    }
}