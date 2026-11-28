package pt.marazul.identity.application;

public record LoginResult(String token, GuestSession session) { }
