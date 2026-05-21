package pt.ipvc.ersc.blockchain.app;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.InvalidChainException;
import pt.ipvc.ersc.blockchain.network.Node;

import java.util.Scanner;

/**
 * Arranque de um único nó para demonstração em rede (1 processo por PC).
 * Responsabilidades:
 * - Inicializar um nó blockchain.
 * - Iniciar o servidor TLS.
 * - Exibir o menu de opções.
 * - Processar as escolhas do utilizador.
 * - Enviar blocos para outro nó.
 * - Validar a cadeia de blocos.
 * - Exibir o status da cadeia.
 */
public class NodeApp {
    // mensagem de erro para o utilizador caso não seja passado o porto, ip ou porta do outro nó.
    private static final String USAGE = """
            Uso: NodeApp --port <porta> --peer-host <ip> --peer-port <porta>
            
            Exemplo (PC A):
              --port 5000 --peer-host 192.168.1.11 --peer-port 5001
            
            Exemplo (PC B):
              --port 5001 --peer-host 192.168.1.10 --peer-port 5000
            """;

    // método main para inicializar o nó blockchain.
    public static void main(String[] args) throws Exception {
        // guarda a configuração do nó blockchain.
        Config config = Config.parse(args);

        // se não for passado o porto, ip ou porta do outro nó, então exibe a mensagem de erro e termina o programa.
        if (config == null) {
            System.err.println(USAGE);
            System.exit(1);
        }

        // cria um novo nó blockchain com o porto passado como argumento.
        Node node = new Node(config.port);

        // inicia o servidor TLS.
        node.startServer();

        // exibe o status do nó blockchain.
        System.out.println("=== Nó blockchain TLS ===");
        System.out.println("Porta local : " + config.port);
        System.out.println("Par (envio) : " + config.peerHost + ":" + config.peerPort);
        System.out.println("Aguarde ~2s para o servidor TLS arrancar...\n");
        Thread.sleep(2000);

        printStatus(node);

        // Cria uma ferramenta para ler o teclado do utilizador, executa o menu usando essa ferramenta e, quando o programa acabar, fecha automaticamente a ligação ao teclado.
        try (Scanner scanner = new Scanner(System.in)) {
            // executa o menu de opções.
            runMenu(node, config, scanner);
        }
    }


    //**
    // recebe o nó blockchain, a configuração e o scanner para ler a entrada do utilizador.
    // executa o menu de opções.
    //**
    private static void runMenu(Node node, Config config, Scanner scanner) {

        while (true) {
            System.out.println("""
                    
                    --- Menu ---
                    1) Enviar mensagem (minera bloco e envia por TLS)
                    2) Estado da cadeia
                    3) Validar cadeia
                    4) Sair
                    Escolha:""");

            // lê a entrada do utilizador e guarda na variável choice sem espaços em branco.
            String choice = scanner.nextLine().trim();


            switch (choice) {
                // caso a escolha seja 1, então envia a mensagem.
                case "1" -> sendMessage(node, config, scanner);
                // caso a escolha seja 2, então exibe o status da cadeia.
                case "2" -> printStatus(node);
                // caso a escolha seja 3, então valida a cadeia.
                case "3" -> validateChain(node);
                // caso a escolha seja 4, então termina o programa.
                case "4" -> {
                    System.out.println("A terminar.");
                    return;
                }
                // caso a escolha seja outra, então exibe uma mensagem de erro.
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    //**
    // recebe o nó blockchain, a configuração e o scanner para ler a entrada do utilizador.
    // envia a mensagem.
    // pede para o utilizador introduzir a mensagem.
    // se a mensagem for vazia, então exibe uma mensagem de erro e retorna.
    // tenta minerar um novo bloco com a mensagem.
    // envia o bloco para o outro nó.
    // exibe o status da cadeia.
    //**
    private static void sendMessage(Node node, Config config, Scanner scanner) {
 
        // pede para o utilizador introduzir a mensagem.
        System.out.print("Mensagem: ");
        String text = scanner.nextLine().trim();

        // se a mensagem for vazia, então exibe uma mensagem de erro e retorna.
        if (text.isEmpty()) {
            System.out.println("Mensagem vazia — cancelado.");
            return;
        }

        // tenta minerar um novo bloco com a mensagem.
        try {
            Block block = node.mineBlock(text);
            System.out.println("Bloco minerado: " + block.getHash());
            // envia o bloco para o outro nó.
            node.sendBlock(block, config.peerHost, config.peerPort);
            // exibe o status da cadeia.
            printStatus(node);
        } catch (InvalidBlockException e) {
            System.out.println("Erro ao minerar: " + e.getMessage());
        }
    }

    //**
    // recebe o nó blockchain.
    // exibe o número de blocos na cadeia.
    // exibe o hash do último bloco.
    // exibe a mensagem do último bloco.
    //**
    private static void printStatus(Node node) {
        // exibe o número de blocos na cadeia.
        System.out.println("Blocos na cadeia: " + node.getBlockchain().size());
        // se a cadeia tiver blocos, então exibe o hash do último bloco e a mensagem do último bloco.
        if (node.getBlockchain().size() > 0) {
            Block latest = node.getBlockchain().getLatestBlock();
            System.out.println("Último hash : " + latest.getHash());
            System.out.println("Último data : " + latest.getData());
        }
    }

    //**
    // recebe o nó blockchain.
    // valida a cadeia.
    // exibe se a cadeia é válida ou inválida.
    //**
    private static void validateChain(Node node) {
        // tenta validar a cadeia.
        try {
            // se a cadeia for válida, então exibe uma mensagem de sucesso.
            boolean ok = node.getBlockchain().isChainValid();
            System.out.println("Cadeia válida? " + ok);
        } catch (InvalidChainException e) {
            // se a cadeia for inválida, então exibe uma mensagem de erro.
            System.out.println("Cadeia inválida: " + e.getMessage());
        }
    }

    //**
    // recebe o porto, ip e porta do outro nó.
    //**
    private record Config(int port, String peerHost, int peerPort) {
        // analisa os argumentos passados para o programa e retorna uma instância de Config.
        static Config parse(String[] args) {

            // inicializa as variáveis para o porto, ip e porta do outro nó.
            Integer port = null;
            String peerHost = null;
            Integer peerPort = null;

            // para cada argumento, verifica se é o porto, ip ou porta do outro nó.
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--port" -> {
                        // se o argumento não for o porto, então retorna null.
                        if (++i >= args.length) return null;
                        // converte o argumento para um número inteiro e guarda na variável port.
                        port = Integer.parseInt(args[i]);
                    }
                    case "--peer-host" -> {
                        // se o argumento não for o ip, então retorna null.
                        if (++i >= args.length) return null;
                        // guarda o argumento na variável peerHost.
                        peerHost = args[i];
                    }
                    case "--peer-port" -> {
                        // se o argumento não for a porta, então retorna null.
                        if (++i >= args.length) return null;
                        // converte o argumento para um número inteiro e guarda na variável peerPort.
                        peerPort = Integer.parseInt(args[i]);
                    }
                    default -> { /* ignorar */ }
                }
            }

            // se o porto, ip ou porta do outro nó for nulo ou em branco, então retorna null.
            if (port == null || peerHost == null || peerPort == null
                    || peerHost.isBlank()) {
                return null;
            }

            // retorna uma instância de Config com o porto, ip e porta do outro nó.
            return new Config(port, peerHost, peerPort);
        }
    }
}
