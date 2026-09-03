package com.ais.marketbackend.proveedores.application.services.impl;

import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import com.ais.marketbackend.proveedores.application.services.interfaces.ProveedorService;
import com.ais.marketbackend.proveedores.domain.exception.ProveedorDuplicadoException;
import com.ais.marketbackend.proveedores.domain.model.Proveedor;
import com.ais.marketbackend.proveedores.domain.repository.ProveedorRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorResumen> obtener(Long id) {
        return proveedorRepository.findById(id).map(this::toResumen);
    }

    @Override
    @Transactional
    public ProveedorResumen crear(String nit, String nombre, String direccion, String telefono, String correo) {
        String nitCanonico = canonicalizarNit(nit);
        if (proveedorRepository.existsByNit(nitCanonico)) {
            throw new ProveedorDuplicadoException(nitCanonico);
        }
        Proveedor proveedor = Proveedor.nuevo(nitCanonico, nombre, direccion, telefono, correo);
        return toResumen(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public ProveedorResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo) {
        Proveedor proveedor = obtenerORequerido(id);
        proveedor.actualizarDatos(nombre, direccion, telefono, correo);
        return toResumen(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Proveedor proveedor = obtenerORequerido(id);
        proveedor.activar();
        proveedorRepository.save(proveedor);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Proveedor proveedor = obtenerORequerido(id);
        proveedor.desactivar();
        proveedorRepository.save(proveedor);
    }

    @Override
    public List<ProveedorResumen> listar() {
        return proveedorRepository.findAll().stream().map(this::toResumen).toList();
    }

    private Proveedor obtenerORequerido(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
    }

    private String canonicalizarNit(String nit) {
        return nit == null ? null : nit.trim().toUpperCase(Locale.ROOT);
    }

    private ProveedorResumen toResumen(Proveedor proveedor) {
        return new ProveedorResumen(
                proveedor.getId(), proveedor.getNit(), proveedor.getNombre(), proveedor.getDireccion(),
                proveedor.getTelefono(), proveedor.getCorreo(), proveedor.getEstado());
    }
}
