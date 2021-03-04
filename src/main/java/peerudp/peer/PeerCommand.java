package peerudp.peer;

public enum PeerCommand {
    EXIT("/exit");

    private String value;

    PeerCommand(String value) {
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
