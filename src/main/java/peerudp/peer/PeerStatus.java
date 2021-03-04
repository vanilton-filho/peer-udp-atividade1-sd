package peerudp.peer;

/**
 * Os status são constantes que podem ser utilizados por um peer para a troca de
 * estados entre eles.
 */
public enum PeerStatus {

    OK("ok.peer"), CLOSE_PEER("close.peer");

    private String value;

    PeerStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
