package pt.ipvc.ersc.blockchain.exception;

/**
 Lançada quando a Blockchain se encontra num estado estruturalmente
 inválido (ex.: cadeia vazia, o que não deve ocorrer em operação normal).
 */
public class InvalidChainException extends Exception {
    public InvalidChainException(String message) {
        super(message);
    }
}