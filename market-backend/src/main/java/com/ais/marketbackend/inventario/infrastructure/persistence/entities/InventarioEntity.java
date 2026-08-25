package com.ais.marketbackend.inventario.infrastructure.persistence.entities;

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
 * {@code tiendaId}/{@code productoId} son columnas planas (no {@code @ManyToOne}):
 * las tablas {@code tienda} y {@code producto} son propiedad de otros módulos.
 */
@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "existencia_actual", nullable = false, precision = 12, scale = 3)
    private BigDecimal existenciaActual;

    @Column(name = "costo_promedio_actual", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoPromedioActual;
}
