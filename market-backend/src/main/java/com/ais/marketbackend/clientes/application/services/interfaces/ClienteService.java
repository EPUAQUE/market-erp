package com.ais.marketbackend.clientes.application.services.interfaces;

import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.shared.domain.Pagina;
import java.math.BigDecimal;

public interface ClienteService {

    /** {@code nit} es opcional — nulo o vacío para clientes de Consumidor Final (CF). */
    ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito);

    /**
     * {@code correlationId} es opcional — sin él, sin protección de idempotencia (el
     * único resguardo contra duplicados sigue siendo {@code existsByNit} para clientes
     * con NIT). Con él, un reintento con los mismos datos bajo la misma clave devuelve
     * el cliente ya creado tal cual — incluida el alta de clientes sin NIT (Consumidor
     * Final variable, o cualquier cliente offline sin NIT capturado), que de otro modo
     * no tiene ninguna deduplicación. El mismo correlationId con datos distintos lanza
     * {@code CorrelationIdReutilizadoException} (409).
     */
    ClienteResumen crear(
            String nit, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito,
            String correlationId);

    ClienteResumen actualizar(
            Long id, String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito);

    /** Usado por otros módulos (Ventas, para validar límite de crédito) — no reemplaza {@link #listar}. */
    ClienteResumen obtener(Long id);

    void activar(Long id);

    void desactivar(Long id);

    Pagina<ClienteResumen> listar(int pagina, int tamano);
}
