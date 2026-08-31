package com.ais.marketbackend.inventario.application.services.impl;

import com.ais.marketbackend.auditoria.infrastructure.aop.Auditable;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.dtos.MovimientoInventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.exception.MovimientoNoPermitidoException;
import com.ais.marketbackend.inventario.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.inventario.domain.model.Inventario;
import com.ais.marketbackend.inventario.domain.model.MovimientoInventario;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.inventario.domain.repository.InventarioRepository;
import com.ais.marketbackend.inventario.domain.repository.MovimientoInventarioRepository;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code productoTiendaService} es una dependencia cruzada de módulo permitida:
 * solo se usa el puerto {@code application.services.interfaces} de Productos, sin
 * tocar sus entidades JPA ni su capa de persistencia.
 */
@Service
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ProductoTiendaService productoTiendaService;
    private final TransactionTemplate transactionTemplate;

    public InventarioServiceImpl(
            InventarioRepository inventarioRepository, MovimientoInventarioRepository movimientoInventarioRepository,
            ProductoTiendaService productoTiendaService, PlatformTransactionManager transactionManager) {
        this.inventarioRepository = inventarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.productoTiendaService = productoTiendaService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Sin {@code @Transactional} propio a nivel de método: el intento de mutación
     * ({@code intentarRegistrarMovimiento}) corre dentro de {@link #transactionTemplate},
     * explícito en vez de vía el proxy de {@code @Transactional} — necesario para poder
     * ejecutar un segundo intento en una transacción realmente nueva y aislada tras una
     * colisión de creación concurrente de la primera fila de inventario, sin depender de
     * auto-invocación dentro de la misma clase (que el proxy de Spring no intercepta).
     * La colisión ocurre solo cuando dos movimientos concurrentes son los primeros sobre
     * un mismo (tienda, producto): {@code PESSIMISTIC_WRITE} serializa todo lo demás. Tras
     * la colisión, la fila ya fue confirmada por la transacción ganadora, así que el
     * reintento la encuentra vía lectura bloqueada y aplica su movimiento como una
     * actualización normal. Un segundo fallo (tienda o producto realmente inexistente)
     * se propaga tal cual — el límite de un solo reintento evita enmascarar errores reales.
     */
    @Override
    @Auditable(accion = "INVENTARIO_AJUSTADO", entidad = "INVENTARIO", tiendaIdParam = "tiendaId",
            entidadIdParam = "productoId")
    public InventarioResumen registrarMovimiento(
            Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
            TipoMovimiento tipoMovimiento) {
        ProductoTiendaResumen configuracion = productoTiendaService.obtener(productoId, tiendaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El producto " + productoId + " no está configurado en la tienda " + tiendaId + "."));

        boolean permiteIngreso = configuracion.activo() && configuracion.permitirIngreso();
        if (tipoMovimiento.esEntrada() && !permiteIngreso) {
            throw new MovimientoNoPermitidoException(
                    "El producto " + productoId + " no admite ingreso de inventario en la tienda " + tiendaId + ".");
        }
        boolean permiteVenta = configuracion.activo() && configuracion.permitirVenta();
        if (tipoMovimiento == TipoMovimiento.VENTA && !permiteVenta) {
            throw new MovimientoNoPermitidoException(
                    "El producto " + productoId + " no admite venta en la tienda " + tiendaId + ".");
        }

        try {
            return transactionTemplate.execute(status ->
                    intentarRegistrarMovimiento(tiendaId, productoId, cantidad, costoUnitario, tipoMovimiento));
        } catch (ReferenciaInvalidaException colisionDeCreacionConcurrente) {
            return transactionTemplate.execute(status ->
                    intentarRegistrarMovimiento(tiendaId, productoId, cantidad, costoUnitario, tipoMovimiento));
        }
    }

    private InventarioResumen intentarRegistrarMovimiento(
            Long tiendaId, Long productoId, BigDecimal cantidad, BigDecimal costoUnitario,
            TipoMovimiento tipoMovimiento) {
        Inventario inventario = inventarioRepository.findByTiendaIdAndProductoIdConBloqueo(tiendaId, productoId)
                .orElseGet(() -> Inventario.nuevo(tiendaId, productoId));

        MovimientoInventario movimiento =
                MovimientoInventario.nuevo(tiendaId, productoId, cantidad, costoUnitario, tipoMovimiento);
        inventario.aplicar(movimiento);

        Inventario guardado = inventarioRepository.save(inventario);
        movimientoInventarioRepository.registrar(movimiento);
        return toResumen(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public InventarioResumen obtener(Long tiendaId, Long productoId) {
        return inventarioRepository.findByTiendaIdAndProductoId(tiendaId, productoId)
                .map(this::toResumen)
                .orElseGet(() -> toResumen(Inventario.nuevo(tiendaId, productoId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventarioResumen> listarPorTienda(Long tiendaId) {
        return inventarioRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<InventarioResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return inventarioRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResumen> listarMovimientos(Long tiendaId, Long productoId) {
        return movimientoInventarioRepository.findByTiendaIdAndProductoIdOrderByFechaDesc(tiendaId, productoId)
                .stream()
                .map(this::toResumen)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<MovimientoInventarioResumen> listarMovimientos(Long tiendaId, Long productoId, int pagina, int tamano) {
        return movimientoInventarioRepository
                .findByTiendaIdAndProductoIdOrderByFechaDesc(tiendaId, productoId, pagina, tamano)
                .map(this::toResumen);
    }

    private InventarioResumen toResumen(Inventario inventario) {
        return new InventarioResumen(
                inventario.getId(), inventario.getTiendaId(), inventario.getProductoId(),
                inventario.getExistenciaActual(), inventario.getCostoPromedioActual());
    }

    private MovimientoInventarioResumen toResumen(MovimientoInventario movimiento) {
        return new MovimientoInventarioResumen(
                movimiento.getId(), movimiento.getFecha(), movimiento.getTiendaId(), movimiento.getProductoId(),
                movimiento.getCantidad(), movimiento.getCostoUnitario(), movimiento.getTipo());
    }
}
