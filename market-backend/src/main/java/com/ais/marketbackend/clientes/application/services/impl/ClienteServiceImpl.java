package com.ais.marketbackend.clientes.application.services.impl;

import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.clientes.domain.exception.ClienteDuplicadoException;
import com.ais.marketbackend.clientes.domain.model.Cliente;
import com.ais.marketbackend.clientes.domain.repository.ClienteRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo,
            BigDecimal limiteCredito) {
        String nitCanonico = canonicalizarNit(nit);
        if (nitCanonico != null && clienteRepository.existsByNit(nitCanonico)) {
            throw new ClienteDuplicadoException(nitCanonico);
        }
        Cliente cliente = Cliente.nuevo(nitCanonico, nombre, direccion, telefono, correo, limiteCredito);
        return toResumen(clienteRepository.save(cliente));
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
    public List<ClienteResumen> listar() {
        return clienteRepository.findAll().stream().map(this::toResumen).toList();
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
