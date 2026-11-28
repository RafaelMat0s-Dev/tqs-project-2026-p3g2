package pt.marazul.identity.domain;

import java.util.UUID;

public record Guest(UUID id, UUID stayId, String name, String codeHash) { }
