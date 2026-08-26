package com.ais.marketbackend.compras.application.services.impl;

import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.LineaCompraResumen;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.compras.domain.model.Compra;
import com.ais.marketbackend.compras.domain.model.LineaCompra;
import com.ais.marketbackend.compras.domain.repository.CompraRepository;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code inventarioService}/{@code cuentaPorPagarService} son dependencias
 * cruzadas de módulo permitidas: solo se usan sus puertos
 * {@code application.services.interfaces}. {@code recibir} registra un
 * movimiento COMPRA por línea y crea la cuenta por pagar del proveedor, todo
 * dentro de la misma transacción que el cambio de estado — si cualquier paso
 * falla (p. ej. Inventario rechaza una línea), toda la operación se revierte y
 * la compra permanece en BORRADOR.
 */
@Service
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final InventarioService inventarioService;
    private final CuentaPorPagarService cuentaPorPagarService;

    public CompraServiceImpl(
            CompraRepository compraRepository, InventarioService inventarioService,
            CuentaPorPagarService cuentaPorPagarService) {
        this.compraRepository = compraRepository;
        this.inventarioService = inventarioService;
        this.cuentaPorPagarService = cuentaPorPagarService;
    }

    @Override
    @Transactional
    public CompraResumen crear(Long tiendaId, Long proveedorId, List<NuevaLineaCompra> lineas) {
        List<LineaCompra> lineasDominio = lineas.stream()
                .map(l -> LineaCompra.nueva(l.productoId(), l.cantidad(), l.costoUnitario()))
                .toList();
        Compra compra = Compra.nueva(proveedorId, tiendaId, lineasDominio);
        return toResumen(compraRepository.save(compra));
    }

    @Override
    @Transactional
    public CompraResumen recibir(Long tiendaId, Long id) {
        Compra compra = obtenerORequerida(tiendaId, id);
        compra.recibir();
        for (LineaCompra linea : compra.getLineas()) {
            inventarioService.registrarMovimiento(
                    tiendaId, linea.getProductoId(), linea.getCantidad(), linea.getCostoUnitario(),
                    TipoMovimiento.COMPRA);
        }
        CompraResumen resumen = toResumen(compraRepository.save(compra));
        cuentaPorPagarService.crear(resumen.id(), resumen.proveedorId(), resumen.tiendaId(), resumen.total());
        return resumen;
    }

    @Override
    @Transactional
    public CompraResumen anular(Long tiendaId, Long id) {
        Compra compra = obtenerORequerida(tiendaId, id);
        compra.anular();
        return toResumen(compraRepository.save(compra));
    }

    @Override
    public CompraResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerida(tiendaId, id));
    }

    @Override
    public List<CompraResumen> listarPorTienda(Long tiendaId) {
        return compraRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<CompraResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return compraRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private Compra obtenerORequerida(Long tiendaId, Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + id));
        if (!compra.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Compra no encontrada: " + id);
        }
        return compra;
    }

    private CompraResumen toResumen(Compra compra) {
        List<LineaCompraResumen> lineas = compra.getLineas().stream()
                .map(l -> new LineaCompraResumen(l.getId(), l.getProductoId(), l.getCantidad(), l.getCostoUnitario()))
                .toList();
        return new CompraResumen(
                compra.getId(), compra.getProveedorId(), compra.getTiendaId(), compra.getFecha(), compra.getEstado(),
                lineas, compra.total());
    }
}
