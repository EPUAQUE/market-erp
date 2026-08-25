package com.ais.marketbackend.clientes.domain.repository;

import com.ais.marketbackend.clientes.domain.model.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(Long id);

    boolean existsByNit(String nit);

    List<Cliente> findAll();
}
