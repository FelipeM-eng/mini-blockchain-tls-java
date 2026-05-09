package pt.ipvc.ersc.blockchain.exception;

/**
 * Lançada quando um bloco candidato não passa nas regras de consenso
 * da Blockchain (encadeamento, integridade ou Proof-of-Work).
 *
 * Checked exception — força o chamador a tratar o erro explicitamente,
 * em vez de o ignorar silenciosamente (comparar com retornar false/null).
 */
public class InvalidBlockException extends Exception {
    public InvalidBlockException(String message) {
        super(message);
    }
}