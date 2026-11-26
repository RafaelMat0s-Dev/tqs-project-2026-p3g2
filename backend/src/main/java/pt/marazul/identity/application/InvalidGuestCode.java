package pt.marazul.identity.application;

public class InvalidGuestCode extends RuntimeException {
    public InvalidGuestCode() {
        super("O código não corresponde a nenhum hóspede.");
    }
}
