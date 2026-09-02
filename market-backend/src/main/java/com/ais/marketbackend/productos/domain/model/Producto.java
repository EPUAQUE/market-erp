package com.ais.marketbackend.productos.domain.model;

import java.util.Objects;

/**
 * Catálogo global de productos. {@code categoriaId}/{@code marcaId}/{@code unidadMedidaId}
 * son identificadores planos hacia otros módulos (Categorías, Marcas, Unidades de
 * Medida) — Productos no importa sus agregados, solo referencia sus ids; la
 * integridad referencial real vive en la FK de PostgreSQL.
 * {@code codigoInterno} es el identificador de negocio, inmutable tras crear.
 */
public class Producto {

    private final Long id;
    private final String codigoInterno;
    private String codigoBarras;
    private String nombre;
    private String descripcion;
    private String descripcionCorta;
    private Long categoriaId;
    private Long marcaId;
    private Long unidadMedidaId;
    private String imagenUrl;
    private boolean activo;

    public Producto(
            Long id, String codigoInterno, String codigoBarras, String nombre, String descripcion,
            String descripcionCorta, Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl,
            boolean activo) {
        this.id = id;
        this.codigoInterno = Objects.requireNonNull(codigoInterno, "codigoInterno");
        this.codigoBarras = codigoBarras;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion;
        this.descripcionCorta = descripcionCorta;
        this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
        this.marcaId = Objects.requireNonNull(marcaId, "marcaId");
        this.unidadMedidaId = Objects.requireNonNull(unidadMedidaId, "unidadMedidaId");
        this.imagenUrl = imagenUrl;
        this.activo = activo;
    }

    public static Producto nuevo(
            String codigoInterno, String codigoBarras, String nombre, String descripcion, String descripcionCorta,
            Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl) {
        return new Producto(null, codigoInterno, codigoBarras, nombre, descripcion, descripcionCorta, categoriaId,
                marcaId, unidadMedidaId, imagenUrl, true);
    }

    public void actualizarDatos(
            String codigoBarras, String nombre, String descripcion, String descripcionCorta, Long categoriaId,
            Long marcaId, Long unidadMedidaId, String imagenUrl) {
        this.codigoBarras = codigoBarras;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion;
        this.descripcionCorta = descripcionCorta;
        this.categoriaId = Objects.requireNonNull(categoriaId, "categoriaId");
        this.marcaId = Objects.requireNonNull(marcaId, "marcaId");
        this.unidadMedidaId = Objects.requireNonNull(unidadMedidaId, "unidadMedidaId");
        this.imagenUrl = imagenUrl;
    }

    public void actualizarImagen(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public Long getMarcaId() {
        return marcaId;
    }

    public Long getUnidadMedidaId() {
        return unidadMedidaId;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public boolean isActivo() {
        return activo;
    }
}
