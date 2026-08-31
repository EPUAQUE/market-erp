package com.ais.marketbackend.productos.application.services.impl;

import com.ais.marketbackend.auditoria.infrastructure.aop.Auditable;
import com.ais.marketbackend.productos.application.dtos.ProductoTiendaResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.productos.domain.exception.ConfiguracionTiendaDuplicadaException;
import com.ais.marketbackend.productos.domain.model.ProductoTienda;
import com.ais.marketbackend.productos.domain.repository.ProductoTiendaRepository;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code obtener(productoId, tiendaId)} es de uso interno entre módulos (ver su
 * Javadoc en la interfaz) y no valida alcance de tienda a propósito — quien
 * invoca ya validó la tienda de la operación que originó la llamada. Los
 * endpoints de cara al usuario (asignar/actualizar/activar/desactivar/listar)
 * sí validan vía {@code AutorizacionTiendaService}.
 *
 * <p>{@code actualizar}/{@code activar}/{@code desactivar} reciben el {@code id}
 * de la configuración, no un {@code tiendaId} elegido por quien llama — igual
 * que en {@code TrasladoServiceImpl}, confirmar primero que existe
 * ({@code obtenerORequerida}) y luego responder {@code 403} por fuera de
 * alcance permitiría distinguir "no existe" de "existe pero no es mía"
 * probando ids. Por eso esas tres traducen la denegación a
 * {@code ResourceNotFoundException} vía {@code exigirAccesoOFingirNoEncontrada}.
 * {@code asignar}/{@code listarPorTienda} reciben el {@code tiendaId}
 * directamente de quien llama — ahí el {@code 403} no filtra nada nuevo.
 */
@Service
public class ProductoTiendaServiceImpl implements ProductoTiendaService {

    private final ProductoTiendaRepository productoTiendaRepository;
    private final AutorizacionTiendaService autorizacionTiendaService;

    public ProductoTiendaServiceImpl(
            ProductoTiendaRepository productoTiendaRepository, AutorizacionTiendaService autorizacionTiendaService) {
        this.productoTiendaRepository = productoTiendaRepository;
        this.autorizacionTiendaService = autorizacionTiendaService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoTiendaResumen> obtener(Long productoId, Long tiendaId) {
        return productoTiendaRepository.findByProductoIdAndTiendaId(productoId, tiendaId).map(this::toResumen);
    }

    @Override
    @Transactional
    @Auditable(accion = "PRODUCTO_TIENDA_ASIGNADO", entidad = "PRODUCTO_TIENDA", tiendaIdParam = "tiendaId",
            entidadIdParam = "productoId")
    public ProductoTiendaResumen asignar(
            Long productoId, Long tiendaId, BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo,
            boolean permitirVenta, boolean permitirIngreso) {
        autorizacionTiendaService.exigirAcceso(tiendaId);
        if (productoTiendaRepository.findByProductoIdAndTiendaId(productoId, tiendaId).isPresent()) {
            throw new ConfiguracionTiendaDuplicadaException(productoId, tiendaId);
        }
        ProductoTienda productoTienda = ProductoTienda.nueva(
                productoId, tiendaId, precioVenta, stockMinimo, stockMaximo, permitirVenta, permitirIngreso);
        return toResumen(productoTiendaRepository.save(productoTienda));
    }

    @Override
    @Transactional
    @Auditable(accion = "PRODUCTO_TIENDA_ACTUALIZADO", entidad = "PRODUCTO_TIENDA", entidadIdParam = "id")
    public ProductoTiendaResumen actualizar(
            Long id, BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo, boolean permitirVenta,
            boolean permitirIngreso) {
        ProductoTienda productoTienda = obtenerORequerida(id);
        exigirAccesoOFingirNoEncontrada(id, productoTienda);
        productoTienda.actualizarConfiguracion(precioVenta, stockMinimo, stockMaximo, permitirVenta, permitirIngreso);
        return toResumen(productoTiendaRepository.save(productoTienda));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        ProductoTienda productoTienda = obtenerORequerida(id);
        exigirAccesoOFingirNoEncontrada(id, productoTienda);
        productoTienda.activar();
        productoTiendaRepository.save(productoTienda);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        ProductoTienda productoTienda = obtenerORequerida(id);
        exigirAccesoOFingirNoEncontrada(id, productoTienda);
        productoTienda.desactivar();
        productoTiendaRepository.save(productoTienda);
    }

    @Override
    public List<ProductoTiendaResumen> listarPorProducto(Long productoId) {
        return productoTiendaRepository.findByProductoId(productoId).stream()
                .filter(pt -> autorizacionTiendaService.tieneAcceso(pt.getTiendaId()))
                .map(this::toResumen)
                .toList();
    }

    @Override
    public List<ProductoTiendaResumen> listarPorTienda(Long tiendaId) {
        autorizacionTiendaService.exigirAcceso(tiendaId);
        return productoTiendaRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<ProductoTiendaResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        autorizacionTiendaService.exigirAcceso(tiendaId);
        return productoTiendaRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    private ProductoTienda obtenerORequerida(Long id) {
        return productoTiendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración producto-tienda no encontrada: " + id));
    }

    private void exigirAccesoOFingirNoEncontrada(Long id, ProductoTienda productoTienda) {
        try {
            autorizacionTiendaService.exigirAcceso(productoTienda.getTiendaId());
        } catch (AccessDeniedException e) {
            throw new ResourceNotFoundException("Configuración producto-tienda no encontrada: " + id);
        }
    }

    private ProductoTiendaResumen toResumen(ProductoTienda productoTienda) {
        return new ProductoTiendaResumen(
                productoTienda.getId(), productoTienda.getProductoId(), productoTienda.getTiendaId(),
                productoTienda.getPrecioVenta(), productoTienda.getStockMinimo(), productoTienda.getStockMaximo(),
                productoTienda.isPermitirVenta(), productoTienda.isPermitirIngreso(), productoTienda.isActivo());
    }
}
