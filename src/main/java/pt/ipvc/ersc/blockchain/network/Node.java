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

//**
// Classe Node - representa um nó na rede blockchain.
// Responsabilidades:
// - Iniciar o servidor TLS.
// - Receber blocos via TLS.
// - Adicionar blocos à blockchain.
// - Enviar blocos via TLS.
// - Obter a blockchain.
// - Obter o porto.
//**
public class Node {

    private final int port;
    private final Blockchain blockchain;
    //**
    // Construtor da classe Node.
    // Responsabilidades:
    // - Inicializar o porto e a blockchain.
    //**
    public Node(int port) {
        // inicializa o porto.
        this.port = port;
        // inicializa a blockchain.
        this.blockchain = new Blockchain();
    }

    //**
    // Inicia o servidor TLS.
    // Responsabilidades:
    // - Criar um novo thread para o servidor TLS.
    // - Aceitar conexões TLS.
    // - Receber blocos via TLS.
    // - Adicionar blocos à blockchain.
    // - Enviar blocos via TLS.
    //**
    public void startServer() {
        // cria um novo thread para o servidor TLS.
        new Thread(() -> {

                // tenta construir o SSLContext para o servidor TLS e captura exceções.
                try {
                        // guarda em serverContext a chave privada do servidor e o seu certificado X.509 (autoassinado).
                        SSLContext serverContext =
                                TLSConfig.buildServerContext();
                        
                        // cria um novo servidor que comunica via tls, utilizando as configuracoes do serverContext.
                        SSLServerSocket serverSocket =
                                (SSLServerSocket)
                                        serverContext
                                                //pede o contexto tls para criar um novo servidor que comunica via tls, utilizando as configuracoes do serverContext.
                                                .getServerSocketFactory()
                                                //abre a porta para receber conexoes.
                                                .createServerSocket(port);

                        // imprime a mensagem de que o servidor TLS está à escuta.
                        System.out.println("[NODE " + port + "] Servidor TLS à escuta...");

                        // loop infinito para receber blocos via TLS.
                        while (true) {

                                // tenta aceitar uma conexão TLS.
                                try (
                                        // espera uma conexão ate que seja aceite. e cria um novo socket(canal direto e exclusivo) para a comunicação.
                                        SSLSocket socket =
                                                (SSLSocket) serverSocket.accept();

                                        // cria em memoria um buffer para ler os dados do socket e guarda em in.
                                        BufferedReader in =
                                                // garante que os dados sao lidos numa linha de texto completa e nao por partes.
                                                new BufferedReader(
                                                        // traduz os bytes do socket para caracteres de texto.
                                                        new InputStreamReader(
                                                                // tras os bytes brutos do socket para o buffer.
                                                                socket.getInputStream()
                                                        )
                                                )
                                ){

                                        // imprime a mensagem de que o cliente TLS está ligado.
                                        System.out.println(
                                                // imprime o porto do nó e o endereço ip do cliente.
                                                "[NODE " + port + "] Cliente TLS ligado: "
                                                        + socket.getInetAddress()
                                        );

                                        // espera uma linha de texto completa do cliente e guarda em message.
                                        String message = in.readLine();

                                        // se a mensagem for nula ou vazia, então imprime uma mensagem de erro e continua o loop.
                                        if (message == null || message.isBlank()) {
                                                System.out.println(
                                                        "[NODE " + port + "] Mensagem vazia"
                                                );
                                                // volta ao inicio do loop.
                                                continue;
                                        }

                                        // se a mensagem for valida, então imprime a mensagem recebida.
                                        System.out.println(
                                                "[NODE " + port + "] Recebido via TLS:"
                                        );
                                        System.out.println(message);

                                        // guarda em receivedBlock os dados da mensagem separado por '|' e converte para um bloco.
                                        Block receivedBlock = parseBlock(message);

                                        // guarda em added se o bloco foi validado (true) ou invalidado (false)
                                        boolean added =
                                                blockchain.addReceivedBlock(receivedBlock);

                                        // se o bloco passou na validação é emitida a respectiva mensagem
                                        if (added) {
                                                System.out.println(
                                                        "[NODE " + port + "] Bloco adicionado!"
                                                );

                                                // e mostra o novo tamanho da blockchain
                                                System.out.println(
                                                        "[NODE " + port + "] Chain size: "
                                                                + blockchain.size()
                                                );

                                        } else {
                                                // caso nao seja validado é emitido a mensagem de erro
                                                System.out.println(
                                                        "[NODE " + port + "] Bloco rejeitado!"
                                                );
                                        }

                                //nó percebe que a mensagem não faz sentido, avisa no terminal o motivo (e.getMessage()) e descarta-a em segurança, sem tentar adicioná-la à blockchain.
                                } catch (MalformedBlockException e) {
                                        System.out.println(
                                                "[NODE " + port + "] Mensagem malformada ignorada: "
                                                        + e.getMessage()
                                        );
                                
                                //O terminal avisa que houve um problema na transmissão ou na segurança do túnel, fecha a ligação defeituosa e liberta os recursos
                                } catch (java.io.IOException e) {
                                        System.out.println(
                                                "[NODE " + port + "] Erro de I/O TLS: "
                                                        + e.getMessage()
                                        );
                                
                                //Se ocorrer um erro inesperado durante o processamento da mensagem, o terminal avisa o tipo e a mensagem do erro, mas continua a funcionar para não comprometer a estabilidade do nó.
                                } catch (RuntimeException e) {

                                        System.out.println(
                                                "[NODE " + port + "] Erro inesperado ("
                                                        + e.getClass().getSimpleName()
                                                        + "): "
                                                        + e.getMessage()
                                        );
                                }
                        }

                // se ocorrer um erro ao iniciar o servidor TLS, o terminal avisa o tipo e a mensagem do erro, e imprime a stack trace para ajudar na depuração.
                } catch (Exception e) {

                        System.out.println(
                                "[NODE " + port + "] Erro ao iniciar servidor TLS."
                        );

                        e.printStackTrace();
                }

        // comando que garante que o thread do servidor TLS seja executado em segundo plano, permitindo que o nó continue a executar outras tarefas simultaneamente.
        }).start();
    }

    //**
    // metodo auxiliar para processar a mensagem recebida via TLS e convertê-la em um objeto Block.
    // Responsabilidades:
    // - Validar o formato da mensagem.
    // - Decodificar os campos da mensagem.
    // - Criar e retornar um objeto Block a partir dos dados decodificados.
    //**
    private Block parseBlock(String message)
            throws MalformedBlockException {
        // verifica se a mensagem é nula ou vazia e lança uma exceção se for o caso.
        if (message == null || message.isBlank()) {
            throw new MalformedBlockException(
                    "Mensagem vazia ou nula."
            );
        }

        // divide a mensagem em partes usando o caractere '|' como separador e guarda em parts.
        String[] parts = message.split("\\|");

        // verifica se o número de partes é igual a 5 e lança uma exceção se não for o caso.
        if (parts.length != 5) {

            throw new MalformedBlockException(
                    "Esperados 5 campos separados por '|'. Recebidos: "
                            + parts.length
            );
        }

        final long timestamp;
        final long nonce;

        // tenta a conversão dos campos de timestamp e nonce para long.
        try {

            timestamp = Long.parseLong(parts[2]);
            nonce     = Long.parseLong(parts[3]);

        // se a conversão falhar, lança uma exceção indicando que os campos não são numéricos.
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
            // decodifica o campo de dados do bloco, que está em Base64, para uma string legível.
            dataDecoded = new String(
                    Base64.getDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );
        
        // se a decodificação falhar, lança uma exceção indicando que o campo de dados não é um Base64 válido.
        } catch (IllegalArgumentException e) {

            throw new MalformedBlockException(
                    "Campo 'data' não é Base64 válido: "
                            + parts[0],
                    e
            );
        }
        
        // se todas as validações passarem, cria e retorna um novo bloco com os dados decodificados e os outros campos.
        return new Block(
                dataDecoded,
                parts[1],
                timestamp,
                nonce,
                parts[4]
        );
    }

    // mina um novo bloco com os dados fornecidos e o adiciona à blockchain.
    public Block mineBlock(String data)
            throws InvalidBlockException {

        return blockchain.addBlock(data);
    }

    //* envia um bloco para outro nó via TLS.
    //recebe o bloco a enviar, o endereço do host de destino e a porta de destino.
    // se o bloco for nulo, imprime uma mensagem de erro e cancela o envio.
    //*//
    public void sendBlock(Block block,
                          String host,
                          int targetPort) {

        // se o bloco for nulo, imprime uma mensagem de erro e cancela o envio.
        if (block == null) {

            System.out.println(
                    "[NODE " + port + "] Bloco nulo. Envio cancelado."
            );

            return;
        }

        
        try {
            // carrega a configuração TLS para o cliente, que inclui os certificados necessários para estabelecer uma conexão segura com o servidor.
            SSLContext clientContext =
                    TLSConfig.buildClientContext();

            try (
                    // realização do handshake TLS no qual trocam-se os certificados e estabelecem-se as chaves de sessão para a comunicação segura.
                    SSLSocket socket =
                            (SSLSocket)
                                    clientContext
                                            //pede o contexto tls para criar um novo socket que comunica via tls, utilizando as configuracoes do clientContext.
                                            .getSocketFactory()
                                            //conecta-se ao host e porta de destino para enviar o bloco.
                                            .createSocket(host, targetPort);

                    // converte o bloco para uma string formatada para a rede e cria um PrintWriter para enviar os dados pelo socket.
                    PrintWriter out =
                            new PrintWriter(
                                    socket.getOutputStream(),
                                    true
                            )
            ) {
                //serializa o bloco para uma string formatada para a rede e envia pelo socket usando o PrintWriter.
                out.println(block.toNetworkString());
                // garante que os dados sejam enviados imediatamente, sem esperar por mais dados para preencher o buffer.
                out.flush();

                // Pequena pausa para o servidor concluir readLine() antes do fecho do socket.
                Thread.sleep(300);

                // imprime a mensagem de que o bloco foi enviado via TLS para a porta de destino.
                System.out.println(
                        "[NODE " + port + "] Bloco enviado via TLS para porta "
                                + targetPort
                );
            }

        } catch (Exception e) {
            // se ocorrer um erro durante o processo de envio, imprime uma mensagem de erro indicando que houve um problema ao enviar o bloco via TLS e exibe a stack trace para ajudar na depuração.
            System.out.println(
                    "[NODE " + port + "] Erro ao enviar bloco via TLS."
            );

            e.printStackTrace();
        }
    }

    // getters para a blockchain
    public Blockchain getBlockchain() {
        return blockchain;
    }

    // getter para o porto do nó.
    public int getPort() {
        return port;
    }
}