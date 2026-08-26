package com.ais.marketbackend.clientes.infrastructure.persistence.adapters;

import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.domain.repository.ClienteRepository;
import com.ais.marketbackend.clientes.infrastructure.persistence.mappers.ClienteEntityMapper;
import com.ais.marketbackend.clientes.infrastructure.persistence.repositories.ClienteJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpaRepository;
    private final ClienteEntityMapper mapper;

    public ClienteRepositoryAdapter(ClienteJpaRepository jpaRepository, ClienteEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Cliente save(Cliente cliente) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(cliente)));
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNit(String nit) {
        return jpaRepository.existsByNit(nit);
    }

    @Override
    public Pagina<Cliente> findAll(int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository.findAll(PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
