package com.ais.marketbackend.tiendas.domain.model;

import java.util.Objects;

/**
 * Agregado raíz de una sucursal. {@code codigo} es el identificador de negocio
 * público y estable — inmutable una vez creada la tienda, a diferencia del resto
 * de sus datos.
 */
public class Tienda {

    private final Long id;
    private final String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private EstadoTienda estado;

    public Tienda(
            Long id, String codigo, String nombre, String direccion, String telefono, String correo,
            EstadoTienda estado) {
        this.id = id;
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static Tienda nueva(String codigo, String nombre, String direccion, String telefono, String correo) {
        return new Tienda(null, codigo, nombre, direccion, telefono, correo, EstadoTienda.ACTIVA);
    }

    public boolean estaActiva() {
        return estado == EstadoTienda.ACTIVA;
    }

    public void actualizarDatos(String nombre, String direccion, String telefono, String correo) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
    }

    public void activar() {
        this.estado = EstadoTienda.ACTIVA;
    }

    public void desactivar() {
        this.estado = EstadoTienda.INACTIVA;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public EstadoTienda getEstado() {
        return estado;
    }
}
