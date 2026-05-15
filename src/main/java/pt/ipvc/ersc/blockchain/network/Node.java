package pt.ipvc.ersc.blockchain.network;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.core.Blockchain;
import pt.ipvc.ersc.blockchain.exception.MalformedBlockException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {

    private final int port;

    // blockchain própria do nó
    private final Blockchain blockchain;

    public Node(int port) {

        this.port = port;

        this.blockchain = new Blockchain();
    }

    // começar servidor do nó
    public void startServer() {

        new Thread(() -> {

            try (ServerSocket serverSocket = new ServerSocket(port)) {

                System.out.println("[NODE " + port + "] À escuta...");

                while (true) {

                    try (
                        Socket socket = serverSocket.accept();

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream())
                        );
                    ) {

                        String message = in.readLine();

                        // proteção básica
                        if (message == null || message.isBlank()) {
                            System.out.println("[NODE " + port + "] Mensagem vazia");
                            continue;
                        }

                        System.out.println("[NODE " + port + "] Recebido:");
                        System.out.println(message);

                        Block receivedBlock = parseBlock(message);

                        // validar + adicionar
                        boolean added = blockchain.addReceivedBlock(receivedBlock);

                        if (added) {

                            System.out.println("[NODE " + port + "] Bloco adicionado!");
                            System.out.println(
                                    "[NODE " + port + "] Chain size: "
                                    + blockchain.size()
                            );

                        } else {

                            System.out.println("[NODE " + port + "] Bloco rejeitado!");
                        }

                    } catch (MalformedBlockException e) {
                        System.out.println("[NODE " + port + "] Mensagem malformada ignorada: "
                            + e.getMessage());
                    } catch (java.io.IOException e) {
                        System.out.println("[NODE " + port + "] Erro de I/O na ligação: "
                            + e.getMessage());
                    } catch (RuntimeException e) {
                        System.out.println("[NODE " + port + "] Erro inesperado ("
                            + e.getClass().getSimpleName() + "): " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    /**
     * Reconstrói um Block a partir da string serializada por Block.toNetworkString.
     * Formato esperado: data|previousHash|timestamp|nonce|hash
     *
     * Lança MalformedBlockException com mensagem específica quando a string
     * está corrompida — para que o chamador possa logar a causa em vez de
     * cair num "Erro ao processar ligação" sem detalhe.
     */
    private Block parseBlock(String message) throws MalformedBlockException {
        if (message == null || message.isBlank()) {
            throw new MalformedBlockException("Mensagem vazia ou nula.");
        }

        String[] parts = message.split("\\|");
        if (parts.length != 5) {
            throw new MalformedBlockException(
                "Esperados 5 campos separados por '|'. Recebidos: " + parts.length
            );
        }

        final long timestamp;
        final int nonce;
        try {
            timestamp = Long.parseLong(parts[2]);
            nonce     = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            throw new MalformedBlockException(
                "Timestamp ou nonce não numéricos: timestamp='" + parts[2] +
                "', nonce='" + parts[3] + "'", e
            );
        }

        return new Block(parts[0], parts[1], timestamp, nonce, parts[4]);
    }

    // minerar bloco localmente
    public Block mineBlock(String data) {

        Block block = new Block(
                data,
                blockchain.getLatestBlock().getHash()
        );

        block.mineBlock(blockchain.getDifficulty());

        // adicionar localmente
        blockchain.addReceivedBlock(block);

        return block;
    }

    // enviar bloco para outro nó
    public void sendBlock(Block block, String host, int targetPort) {

        try (
            Socket socket = new Socket(host, targetPort);

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );
        ) {

            out.println(block.toNetworkString());

            System.out.println(
                    "[NODE " + port + "] Bloco enviado para porta "
                    + targetPort
            );

        } catch (Exception e) {

            System.out.println(
                    "[NODE " + port + "] Erro ao enviar bloco"
            );
        }
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public int getPort() {
        return port;
    }
}