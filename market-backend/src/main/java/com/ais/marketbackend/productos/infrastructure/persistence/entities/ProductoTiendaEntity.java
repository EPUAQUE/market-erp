package com.ais.marketbackend.productos.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code tiendaId} es una columna plana (no {@code @ManyToOne}): la tabla
 * {@code tienda} es propiedad del módulo Tiendas. {@code productoId} tampoco usa
 * relación JPA, por simetría y porque el mapper de dominio ya trabaja con ids.
 */
@Entity
@Table(name = "producto_tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductoTiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "stock_maximo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMaximo;

    @Column(name = "permitir_venta", nullable = false)
    private boolean permitirVenta;

    @Column(name = "permitir_ingreso", nullable = false)
    private boolean permitirIngreso;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
