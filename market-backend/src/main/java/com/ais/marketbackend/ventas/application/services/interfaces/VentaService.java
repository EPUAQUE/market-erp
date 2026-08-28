package com.ais.marketbackend.ventas.application.services.interfaces;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.PagoInmediato;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.util.List;
import java.util.Optional;

public interface VentaService {

    /**
     * {@code correlationId} es opcional — solo lo manda el cliente al
     * sincronizar una venta creada offline. La clave de idempotencia es
     * compuesta: {@code (tiendaId, vendedorId, correlationId)} — el mismo
     * valor puede reutilizarse sin colisión entre tiendas o vendedores
     * distintos. Un reintento con el mismo cliente/líneas/método que una
     * venta ya registrada bajo esa clave la devuelve tal cual (idempotencia
     * real); el mismo {@code correlationId} con contenido distinto lanza
     * {@code CorrelationIdReutilizadoException} (409) en vez de devolver
     * silenciosamente la venta equivocada.
     */
    VentaResumen crear(
            Long tiendaId, Long clienteId, Long vendedorId, List<NuevaLineaVenta> lineas, MetodoPago metodoPago,
            String correlationId);

    /**
     * Transiciona BORRADOR -> COMPLETADA, registra un movimiento VENTA en Inventario
     * por cada línea, y — según {@code Venta.metodoPago} — refleja el ingreso
     * inmediato en Caja y/o crea una cuenta por cobrar únicamente por el saldo no
     * cubierto. Equivalente a llamar a la sobrecarga de abajo sin pagos inmediatos
     * (correcto para EFECTIVO/TARJETA/TRANSFERENCIA/CREDITO — el desglose solo lo
     * exige MIXTO).
     */
    VentaResumen completar(Long tiendaId, Long id);

    /**
     * {@code pagosInmediatos} solo es obligatorio (y se valida) cuando la venta es
     * {@code MIXTO} — para cualquier otro método se ignora: EFECTIVO/TARJETA/
     * TRANSFERENCIA ya implican un único pago inmediato por el total, y CREDITO
     * implica ninguno. Ver {@code VentaServiceImpl.completar} para las reglas
     * exactas de caja/cuenta por cobrar por método.
     */
    VentaResumen completar(Long tiendaId, Long id, List<PagoInmediato> pagosInmediatos);

    VentaResumen anular(Long tiendaId, Long id);

    VentaResumen obtener(Long tiendaId, Long id);

    /**
     * Resuelve una respuesta incierta (timeout, caída de red tras el request) sin
     * volver a emitir a ciegas: el cliente reintenta primero esta consulta y solo
     * hace {@code POST} de nuevo si viene vacía.
     */
    Optional<VentaResumen> buscarPorCorrelationId(Long tiendaId, Long vendedorId, String correlationId);

    /** Sin paginar — uso interno (ej. agregados del dashboard). El endpoint público usa la variante paginada. */
    List<VentaResumen> listarPorTienda(Long tiendaId);

    Pagina<VentaResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
