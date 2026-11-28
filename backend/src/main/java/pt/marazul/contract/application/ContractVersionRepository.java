package pt.marazul.contract.application;

import pt.marazul.contract.domain.ContractVersion;

import java.util.List;

public interface ContractVersionRepository {
    List<ContractVersion> findAll();

    void save(ContractVersion version);
}
