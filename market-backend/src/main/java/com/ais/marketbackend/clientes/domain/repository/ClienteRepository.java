package com.ais.marketbackend.clientes.domain.repository;

import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.Optional;

public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(Long id);

    boolean existsByNit(String nit);

    Optional<Cliente> findByCorrelationId(String correlationId);

    Pagina<Cliente> findAll(int pagina, int tamano);
}
