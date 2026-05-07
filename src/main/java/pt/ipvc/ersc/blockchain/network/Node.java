package pt.ipvc.ersc.blockchain.network;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.core.Blockchain;

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
                                    + blockchain.chain.size()
                            );

                        } else {

                            System.out.println("[NODE " + port + "] Bloco rejeitado!");
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "[NODE " + port + "] Erro ao processar ligação"
                        );
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    // converter string recebida em bloco
    private Block parseBlock(String message) {

        String[] parts = message.split("\\|");

        if (parts.length != 5) {
            throw new RuntimeException("Formato inválido");
        }

        return new Block(
                parts[0],
                parts[1],
                Long.parseLong(parts[2]),
                Integer.parseInt(parts[3]),
                parts[4]
        );
    }

    // minerar bloco localmente
    public Block mineBlock(String data) {

        Block block = new Block(
                data,
                blockchain.getLatestBlock().hash
        );

        block.mineBlock(blockchain.difficulty);

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