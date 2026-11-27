package pt.marazul.support;

import pt.marazul.contract.application.ContractVersionRepository;
import pt.marazul.contract.domain.ContractVersion;

import java.util.ArrayList;
import java.util.List;

public class InMemoryContractVersionRepository implements ContractVersionRepository {
    private final List<ContractVersion> versions = new ArrayList<>(List.of(ContractFixtures.v1()));

    @Override
    public List<ContractVersion> findAll() {
        return List.copyOf(versions);
    }

    @Override
    public void save(ContractVersion version) {
        versions.add(version);
    }
}
