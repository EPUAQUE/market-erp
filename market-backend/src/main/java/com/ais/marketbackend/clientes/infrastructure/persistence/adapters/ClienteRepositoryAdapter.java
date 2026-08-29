package com.ais.marketbackend.clientes.infrastructure.persistence.adapters;

import com.ais.marketbackend.clientes.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.domain.repository.ClienteRepository;
import com.ais.marketbackend.clientes.infrastructure.persistence.mappers.ClienteEntityMapper;
import com.ais.marketbackend.clientes.infrastructure.persistence.repositories.ClienteJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(cliente)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("El NIT o el correlationId ya están en uso por otro cliente.");
        }
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Cliente> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNit(String nit) {
        return jpaRepository.existsByNit(nit);
    }

    @Override
    public Optional<Cliente> findByCorrelationId(String correlationId) {
        return jpaRepository.findByCorrelationId(correlationId).map(mapper::toDomain);
    }

    @Override
    public Pagina<Cliente> findAll(int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository.findAll(PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
