package com.ais.marketbackend.ventas.domain.repository;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.ventas.domain.model.Venta;
import java.util.List;
import java.util.Optional;

public interface VentaRepository {

    Venta save(Venta venta);

    Optional<Venta> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code VentaServiceImpl.completar}/{@code anular} para serializar
     * transiciones de estado concurrentes sobre la misma venta (mismo motivo
     * que {@code CompraRepository.findByIdConBloqueo}/
     * {@code TrasladoRepository.findByIdConBloqueo}). Sin esto, dos
     * {@code completar} casi simultáneos (o un {@code completar} contra un
     * {@code anular}) podían ambos leer BORRADOR y pasar la validación,
     * duplicando el movimiento de Inventario/CxC/Caja o dejando la venta
     * ANULADA con esos efectos ya aplicados.
     */
    Optional<Venta> findByIdConBloqueo(Long id);

    /**
     * La clave de idempotencia es compuesta (tienda + vendedor + correlationId) —
     * nunca buscar solo por {@code correlationId}, dos tiendas legítimas pueden
     * reutilizar el mismo valor sin colisionar entre sí (ver
     * {@code VentaServiceImpl.crear}).
     */
    Optional<Venta> findByTiendaIdAndVendedorIdAndCorrelationId(Long tiendaId, Long vendedorId, String correlationId);

    /** Sin paginar — uso interno (ej. agregados del dashboard), no exponer tal cual en un endpoint público. */
    List<Venta> findByTiendaId(Long tiendaId);

    Pagina<Venta> findByTiendaId(Long tiendaId, int pagina, int tamano);
}
