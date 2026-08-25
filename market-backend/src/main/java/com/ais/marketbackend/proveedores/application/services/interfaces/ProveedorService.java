package com.ais.marketbackend.proveedores.application.services.interfaces;

import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import java.util.List;

public interface ProveedorService {

    ProveedorResumen crear(String nit, String nombre, String direccion, String telefono, String correo);

    ProveedorResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo);

    void activar(Long id);

    void desactivar(Long id);

    List<ProveedorResumen> listar();
}
