package com.ais.marketbackend.productos.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code categoriaId}/{@code marcaId}/{@code unidadMedidaId} son columnas planas
 * (no {@code @ManyToOne}): esas entidades pertenecen a otros módulos. La FK real
 * vive en el changelog de este módulo (productos/001-producto.xml).
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_interno", nullable = false, unique = true, length = 40)
    private String codigoInterno;

    @Column(name = "codigo_barras", unique = true, length = 40)
    private String codigoBarras;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    /** Para imprimir en factura/recibo y para mostrar en el POS de Flutter — más corta que {@code descripcion}. */
    @Column(name = "descripcion_corta", length = 100)
    private String descripcionCorta;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(name = "marca_id", nullable = false)
    private Long marcaId;

    @Column(name = "unidad_medida_id", nullable = false)
    private Long unidadMedidaId;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "activo", nullable = false)
    private boolean activo;
}
