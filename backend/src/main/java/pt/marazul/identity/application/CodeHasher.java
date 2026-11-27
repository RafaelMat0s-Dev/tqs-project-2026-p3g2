package pt.marazul.identity.application;

public interface CodeHasher {
    String hash(String code);
}
