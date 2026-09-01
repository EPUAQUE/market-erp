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

    /**
     * Resuelve el cliente "Consumidor Final" sembrado por Liquibase
     * ({@code clientes/001-cliente.xml}) por nombre exacto, no por su id de fila
     * (Fase 2, PLAN_MEJORAS.md: antes los clientes HTTP —
     * {@code market-flutter}— asumían el id fijo {@code 1}, correcto solo por
     * ser la primera fila insertada en una BD nueva, no un contrato real).
     * Mismo patrón que {@code RolRepository.findByNombre} para roles sembrados.
     */
    Optional<Cliente> findByNombre(String nombre);

    Pagina<Cliente> findAll(int pagina, int tamano);
}
