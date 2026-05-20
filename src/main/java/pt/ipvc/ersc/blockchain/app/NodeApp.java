package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;
import pt.ipvc.ersc.blockchain.network.Node;

import java.util.Scanner;

/**
 * Arranque de um único nó para demonstração em rede (1 processo por PC).
 *
 * Uso:
 *   --port &lt;porta local&gt;
 *   --peer-host &lt;IP do outro nó blockchain&gt;
 *   --peer-port &lt;porta do outro nó&gt;
 *
 * Exemplo no PC A:
 *   mvn exec:java -Dexec.mainClass=pt.ipvc.ersc.blockchain.app.NodeApp
 *     "-Dexec.args=--port 5000 --peer-host 192.168.1.11 --peer-port 5001"
 */
public class NodeApp {

    private static final String USAGE = """
            Uso: NodeApp --port <porta> --peer-host <ip> --peer-port <porta>
            
            Exemplo (PC A):
              --port 5000 --peer-host 192.168.1.11 --peer-port 5001
            
            Exemplo (PC B):
              --port 5001 --peer-host 192.168.1.10 --peer-port 5000
            """;

    public static void main(String[] args) throws Exception {

        Config config = Config.parse(args);
        if (config == null) {
            System.err.println(USAGE);
            System.exit(1);
        }

        Node node = new Node(config.port);
        node.startServer();

        System.out.println("=== Nó blockchain TLS ===");
        System.out.println("Porta local : " + config.port);
        System.out.println("Par (envio) : " + config.peerHost + ":" + config.peerPort);
        System.out.println("Aguarde ~2s para o servidor TLS arrancar...\n");
        Thread.sleep(2000);

        printStatus(node);

        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(node, config, scanner);
        }
    }

    private static void runMenu(Node node, Config config, Scanner scanner) {

        while (true) {
            System.out.println("""
                    
                    --- Menu ---
                    1) Enviar mensagem (minera bloco e envia por TLS)
                    2) Estado da cadeia
                    3) Validar cadeia
                    4) Sair
                    Escolha:""");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> sendMessage(node, config, scanner);
                case "2" -> printStatus(node);
                case "3" -> validateChain(node);
                case "4" -> {
                    System.out.println("A terminar.");
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private static void sendMessage(Node node, Config config, Scanner scanner) {

        System.out.print("Mensagem: ");
        String text = scanner.nextLine().trim();

        if (text.isEmpty()) {
            System.out.println("Mensagem vazia — cancelado.");
            return;
        }

        try {
            Block block = node.mineBlock(text);
            System.out.println("Bloco minerado: " + block.getHash());
            node.sendBlock(block, config.peerHost, config.peerPort);
            printStatus(node);
        } catch (InvalidBlockException e) {
            System.out.println("Erro ao minerar: " + e.getMessage());
        }
    }

    private static void printStatus(Node node) {
        System.out.println("Blocos na cadeia: " + node.getBlockchain().size());
        if (node.getBlockchain().size() > 0) {
            Block latest = node.getBlockchain().getLatestBlock();
            System.out.println("Último hash : " + latest.getHash());
            System.out.println("Último data : " + latest.getData());
        }
    }

    private static void validateChain(Node node) {
        try {
            boolean ok = node.getBlockchain().isChainValid();
            System.out.println("Cadeia válida? " + ok);
        } catch (InvalidChainException e) {
            System.out.println("Cadeia inválida: " + e.getMessage());
        }
    }

    private record Config(int port, String peerHost, int peerPort) {

        static Config parse(String[] args) {

            Integer port = null;
            String peerHost = null;
            Integer peerPort = null;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--port" -> {
                        if (++i >= args.length) return null;
                        port = Integer.parseInt(args[i]);
                    }
                    case "--peer-host" -> {
                        if (++i >= args.length) return null;
                        peerHost = args[i];
                    }
                    case "--peer-port" -> {
                        if (++i >= args.length) return null;
                        peerPort = Integer.parseInt(args[i]);
                    }
                    default -> { /* ignorar */ }
                }
            }

            if (port == null || peerHost == null || peerPort == null
                    || peerHost.isBlank()) {
                return null;
            }

            return new Config(port, peerHost, peerPort);
        }
    }
}
