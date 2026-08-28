package com.ais.marketbackend.clientes.application.services.impl;

import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.clientes.domain.exception.ClienteDuplicadoException;
import com.ais.marketbackend.clientes.domain.exception.CorrelationIdReutilizadoException;
import com.ais.marketbackend.clientes.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.domain.repository.ClienteRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code crear} es deliberadamente SIN {@code @Transactional} propio cuando recibe
 * {@code correlationId} — mismo motivo que {@code VentaServiceImpl.crear}: tras una
 * colisión de restricción única, la sesión de Hibernate que acaba de fallar el flush
 * queda inutilizable para releer en la misma transacción.
 */
@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo,
            BigDecimal limiteCredito) {
        return crear(nit, nombre, direccion, telefono, correo, limiteCredito, null);
    }

    @Override
    public ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito,
            String correlationId) {
        String nitCanonico = canonicalizarNit(nit);
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        if (correlationIdNormalizado != null) {
            Optional<Cliente> existente = clienteRepository.findByCorrelationId(correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverAltaIdempotente(
                        existente.get(), nitCanonico, nombre, direccion, telefono, correo, limiteCredito);
            }
        }
        if (nitCanonico != null && clienteRepository.existsByNit(nitCanonico)) {
            throw new ClienteDuplicadoException(nitCanonico);
        }
        Cliente cliente =
                Cliente.nuevo(nitCanonico, nombre, direccion, telefono, correo, limiteCredito, correlationIdNormalizado);
        try {
            return toResumen(clienteRepository.save(cliente));
        } catch (ReferenciaInvalidaException e) {
            // Dos altas concurrentes con el mismo correlationId: la que pierde la
            // carrera choca contra la restricción única — releer antes de decidir si
            // era la carrera esperada o una referencia realmente inválida (o un NIT
            // duplicado por una carrera distinta, que no encontrará nada por
            // correlationId y se relanza tal cual), igual que VentaServiceImpl.crear.
            if (correlationIdNormalizado != null) {
                Optional<Cliente> existente = clienteRepository.findByCorrelationId(correlationIdNormalizado);
                if (existente.isPresent()) {
                    return resolverAltaIdempotente(
                            existente.get(), nitCanonico, nombre, direccion, telefono, correo, limiteCredito);
                }
            }
            throw e;
        }
    }

    private ClienteResumen resolverAltaIdempotente(
            Cliente existente, String nitCanonico, String nombre, String direccion, String telefono, String correo,
            BigDecimal limiteCredito) {
        boolean coincide = Objects.equals(existente.getNit(), nitCanonico)
                && Objects.equals(existente.getNombre(), nombre)
                && Objects.equals(existente.getDireccion(), direccion)
                && Objects.equals(existente.getTelefono(), telefono)
                && Objects.equals(existente.getCorreo(), correo)
                && montosIguales(existente.getLimiteCredito(), limiteCredito);
        if (!coincide) {
            throw new CorrelationIdReutilizadoException(existente.getCorrelationId());
        }
        return toResumen(existente);
    }

    private boolean montosIguales(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }

    /** {@code null}/en blanco se tratan igual — sin correlationId, sin idempotencia. */
    private String normalizarCorrelationId(String correlationId) {
        if (correlationId == null) {
            return null;
        }
        String recortado = correlationId.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    @Override
    @Transactional
    public ClienteResumen actualizar(
            Long id, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito) {
        Cliente cliente = obtenerORequerido(id);
        cliente.actualizarDatos(nombre, direccion, telefono, correo, limiteCredito);
        return toResumen(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Cliente cliente = obtenerORequerido(id);
        cliente.activar();
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Cliente cliente = obtenerORequerido(id);
        cliente.desactivar();
        clienteRepository.save(cliente);
    }

    @Override
    public Pagina<ClienteResumen> listar(int pagina, int tamano) {
        return clienteRepository.findAll(pagina, tamano).map(this::toResumen);
    }

    @Override
    public ClienteResumen obtener(Long id) {
        return toResumen(obtenerORequerido(id));
    }

    private Cliente obtenerORequerido(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    private String canonicalizarNit(String nit) {
        if (nit == null || nit.isBlank()) {
            return null;
        }
        return nit.trim().toUpperCase(Locale.ROOT);
    }

    private ClienteResumen toResumen(Cliente cliente) {
        return new ClienteResumen(
                cliente.getId(), cliente.getNit(), cliente.getNombre(), cliente.getDireccion(),
                cliente.getTelefono(), cliente.getCorreo(), cliente.getEstado(), cliente.getLimiteCredito());
    }
}
