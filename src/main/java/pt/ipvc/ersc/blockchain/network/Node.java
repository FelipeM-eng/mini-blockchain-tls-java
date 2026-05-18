package pt.ipvc.ersc.blockchain.network;

import pt.ipvc.ersc.blockchain.core.Block;
import pt.ipvc.ersc.blockchain.core.Blockchain;
import pt.ipvc.ersc.blockchain.exception.InvalidBlockException;
import pt.ipvc.ersc.blockchain.exception.MalformedBlockException;
import pt.ipvc.ersc.blockchain.security.TLSConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Node {

    private final int port;
    private final Blockchain blockchain;

    public Node(int port) {
        this.port = port;
        this.blockchain = new Blockchain();
    }

    public void startServer() {

        new Thread(() -> {

            try {

                SSLContext serverContext =
                        TLSConfig.buildServerContext();

                SSLServerSocket serverSocket =
                        (SSLServerSocket)
                                serverContext
                                        .getServerSocketFactory()
                                        .createServerSocket(port);

                System.out.println("[NODE " + port + "] Servidor TLS à escuta...");

                while (true) {

                    try (
                            SSLSocket socket =
                                    (SSLSocket) serverSocket.accept();

                            BufferedReader in =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    socket.getInputStream()
                                            )
                                    )
                    ) {

                        System.out.println(
                                "[NODE " + port + "] Cliente TLS ligado: "
                                        + socket.getInetAddress()
                        );

                        String message = in.readLine();

                        if (message == null || message.isBlank()) {
                            System.out.println(
                                    "[NODE " + port + "] Mensagem vazia"
                            );
                            continue;
                        }

                        System.out.println(
                                "[NODE " + port + "] Recebido via TLS:"
                        );
                        System.out.println(message);

                        Block receivedBlock = parseBlock(message);

                        boolean added =
                                blockchain.addReceivedBlock(receivedBlock);

                        if (added) {

                            System.out.println(
                                    "[NODE " + port + "] Bloco adicionado!"
                            );

                            System.out.println(
                                    "[NODE " + port + "] Chain size: "
                                            + blockchain.size()
                            );

                        } else {

                            System.out.println(
                                    "[NODE " + port + "] Bloco rejeitado!"
                            );
                        }

                    } catch (MalformedBlockException e) {

                        System.out.println(
                                "[NODE " + port + "] Mensagem malformada ignorada: "
                                        + e.getMessage()
                        );

                    } catch (java.io.IOException e) {

                        System.out.println(
                                "[NODE " + port + "] Erro de I/O TLS: "
                                        + e.getMessage()
                        );

                    } catch (RuntimeException e) {

                        System.out.println(
                                "[NODE " + port + "] Erro inesperado ("
                                        + e.getClass().getSimpleName()
                                        + "): "
                                        + e.getMessage()
                        );
                    }
                }

            } catch (Exception e) {

                System.out.println(
                        "[NODE " + port + "] Erro ao iniciar servidor TLS."
                );

                e.printStackTrace();
            }

        }).start();
    }

    private Block parseBlock(String message)
            throws MalformedBlockException {

        if (message == null || message.isBlank()) {
            throw new MalformedBlockException(
                    "Mensagem vazia ou nula."
            );
        }

        String[] parts = message.split("\\|");

        if (parts.length != 5) {

            throw new MalformedBlockException(
                    "Esperados 5 campos separados por '|'. Recebidos: "
                            + parts.length
            );
        }

        final long timestamp;
        final long nonce;

        try {

            timestamp = Long.parseLong(parts[2]);
            nonce     = Long.parseLong(parts[3]);

        } catch (NumberFormatException e) {

            throw new MalformedBlockException(
                    "Timestamp ou nonce não numéricos: timestamp='"
                            + parts[2]
                            + "', nonce='"
                            + parts[3]
                            + "'",
                    e
            );
        }

        final String dataDecoded;

        try {

            dataDecoded = new String(
                    Base64.getDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );

        } catch (IllegalArgumentException e) {

            throw new MalformedBlockException(
                    "Campo 'data' não é Base64 válido: "
                            + parts[0],
                    e
            );
        }

        return new Block(
                dataDecoded,
                parts[1],
                timestamp,
                nonce,
                parts[4]
        );
    }

    public Block mineBlock(String data)
            throws InvalidBlockException {

        return blockchain.addBlock(data);
    }

    public void sendBlock(Block block,
                          String host,
                          int targetPort) {

        if (block == null) {

            System.out.println(
                    "[NODE " + port + "] Bloco nulo. Envio cancelado."
            );

            return;
        }

        try {

            SSLContext clientContext =
                    TLSConfig.buildClientContext();

            try (
                    SSLSocket socket =
                            (SSLSocket)
                                    clientContext
                                            .getSocketFactory()
                                            .createSocket(host, targetPort);

                    PrintWriter out =
                            new PrintWriter(
                                    socket.getOutputStream(),
                                    true
                            )
            ) {

                out.println(block.toNetworkString());

                System.out.println(
                        "[NODE " + port + "] Bloco enviado via TLS para porta "
                                + targetPort
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "[NODE " + port + "] Erro ao enviar bloco via TLS."
            );

            e.printStackTrace();
        }
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public int getPort() {
        return port;
    }
}