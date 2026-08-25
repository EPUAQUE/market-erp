package com.ais.marketbackend.productos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Configuración de un producto en una tienda concreta: precio, umbrales de stock
 * y las banderas de negocio que gobiernan el movimiento de inventario.
 *
 * <p>Regla de negocio: si {@code permitirIngreso} es {@code false}, el producto
 * <b>no</b> puede recibir inventario vía compras, traslados o ajustes positivos en
 * esa tienda — pero sigue siendo vendible con la existencia que ya tenga. Esta
 * clase solo modela y expone la bandera; la validación en el momento del
 * movimiento la aplica el módulo Inventario al consultar esta configuración.
 */
public class ProductoTienda {

    private final Long id;
    private final Long productoId;
    private final Long tiendaId;
    private BigDecimal precioVenta;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;
    private boolean permitirVenta;
    private boolean permitirIngreso;
    private boolean activo;

    public ProductoTienda(
            Long id, Long productoId, Long tiendaId, BigDecimal precioVenta, BigDecimal stockMinimo,
            BigDecimal stockMaximo, boolean permitirVenta, boolean permitirIngreso, boolean activo) {
        this.id = id;
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.permitirVenta = permitirVenta;
        this.permitirIngreso = permitirIngreso;
        this.activo = activo;
        aplicarPrecioYStock(precioVenta, stockMinimo, stockMaximo);
    }

    public static ProductoTienda nueva(
            Long productoId, Long tiendaId, BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo,
            boolean permitirVenta, boolean permitirIngreso) {
        return new ProductoTienda(
                null, productoId, tiendaId, precioVenta, stockMinimo, stockMaximo, permitirVenta, permitirIngreso,
                true);
    }

    public void actualizarConfiguracion(
            BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo, boolean permitirVenta,
            boolean permitirIngreso) {
        aplicarPrecioYStock(precioVenta, stockMinimo, stockMaximo);
        this.permitirVenta = permitirVenta;
        this.permitirIngreso = permitirIngreso;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean permiteIngresoDeInventario() {
        return activo && permitirIngreso;
    }

    public boolean permiteVenta() {
        return activo && permitirVenta;
    }

    private void aplicarPrecioYStock(BigDecimal precioVenta, BigDecimal stockMinimo, BigDecimal stockMaximo) {
        Objects.requireNonNull(precioVenta, "precioVenta");
        Objects.requireNonNull(stockMinimo, "stockMinimo");
        Objects.requireNonNull(stockMaximo, "stockMaximo");
        if (precioVenta.signum() < 0) {
            throw new IllegalArgumentException("El precio de venta no puede ser negativo.");
        }
        if (stockMinimo.signum() < 0 || stockMaximo.signum() < 0) {
            throw new IllegalArgumentException("Los umbrales de stock no pueden ser negativos.");
        }
        if (stockMinimo.compareTo(stockMaximo) > 0) {
            throw new IllegalArgumentException("El stock mínimo no puede superar al stock máximo.");
        }
        this.precioVenta = precioVenta;
        this.stockMinimo = stockMinimo;
        this.stockMaximo = stockMaximo;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public BigDecimal getStockMaximo() {
        return stockMaximo;
    }

    public boolean isPermitirVenta() {
        return permitirVenta;
    }

    public boolean isPermitirIngreso() {
        return permitirIngreso;
    }

    public boolean isActivo() {
        return activo;
    }
}
