package pt.ipvc.ersc.blockchain.exception;

/**
 * Lançada quando uma mensagem recebida pela rede não pode ser interpretada
 * como um bloco válido — número errado de campos, valores numéricos
 * malformados, ou outra violação do formato de serialização definido em
 * Block.toNetworkString.
 *
 * Distinta de InvalidBlockException: aqui o problema é sintáctico (não
 * conseguimos sequer reconstruir o objecto Block); naquela o bloco existe
 * mas falha as regras de consenso (encadeamento, PoW, integridade).
 *
 * Responsável: Samuel Ferreira (33846).
 */
public class MalformedBlockException extends Exception {

    public MalformedBlockException(String message) {
        super(message);
    }

    public MalformedBlockException(String message, Throwable cause) {
        super(message, cause);
    }
}