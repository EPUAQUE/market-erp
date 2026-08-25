package com.ais.marketbackend.clientes.application.services.interfaces;

import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import java.math.BigDecimal;
import java.util.List;

public interface ClienteService {

    /** {@code nit} es opcional — nulo o vacío para clientes de Consumidor Final (CF). */
    ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito);

    ClienteResumen actualizar(
            Long id, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito);

    /** Usado por otros módulos (Ventas, para validar límite de crédito) — no reemplaza {@link #listar()}. */
    ClienteResumen obtener(Long id);

    void activar(Long id);

    void desactivar(Long id);

    List<ClienteResumen> listar();
}
