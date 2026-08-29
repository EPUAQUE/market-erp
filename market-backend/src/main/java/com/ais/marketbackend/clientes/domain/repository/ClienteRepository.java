package com.ais.marketbackend.clientes.domain.repository;

import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.Optional;

public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado para
     * serializar la validación de límite de crédito entre ventas concurrentes
     * del mismo cliente (ver {@code VentaServiceImpl.validarLimiteCredito}).
     * Sin esto, dos ventas a crédito casi simultáneas del mismo cliente pueden
     * leer el mismo saldo pendiente y juntas exceder el límite aunque cada una
     * individualmente no lo haga.
     */
    Optional<Cliente> findByIdConBloqueo(Long id);

    boolean existsByNit(String nit);

    Optional<Cliente> findByCorrelationId(String correlationId);

    Pagina<Cliente> findAll(int pagina, int tamano);
}
