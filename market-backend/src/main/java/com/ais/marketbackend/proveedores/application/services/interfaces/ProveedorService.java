package com.ais.marketbackend.proveedores.application.services.interfaces;

import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import java.util.List;
import java.util.Optional;

public interface ProveedorService {

    Optional<ProveedorResumen> obtener(Long id);

    ProveedorResumen crear(String nit, String nombre, String direccion, String telefono, String correo);

    ProveedorResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo);

    void activar(Long id);

    void desactivar(Long id);

    List<ProveedorResumen> listar();
}
