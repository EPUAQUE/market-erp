package com.ais.marketbackend.proveedores.domain.repository;

import com.ais.marketbackend.proveedores.domain.model.Proveedor;
import java.util.List;
import java.util.Optional;

public interface ProveedorRepository {

    Proveedor save(Proveedor proveedor);

    Optional<Proveedor> findById(Long id);

    boolean existsByNit(String nit);

    List<Proveedor> findAll();
}
