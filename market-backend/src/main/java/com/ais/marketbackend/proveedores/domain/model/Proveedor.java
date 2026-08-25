package com.ais.marketbackend.proveedores.domain.model;

import java.util.Objects;

/**
 * Agregado raíz de un proveedor. {@code nit} es el identificador fiscal — inmutable
 * una vez creado el proveedor, a diferencia del resto de sus datos.
 */
public class Proveedor {

    private final Long id;
    private final String nit;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private EstadoProveedor estado;

    public Proveedor(
            Long id, String nit, String nombre, String direccion, String telefono, String correo,
            EstadoProveedor estado) {
        this.id = id;
        this.nit = Objects.requireNonNull(nit, "nit");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = Objects.requireNonNull(estado, "estado");
    }

    public static Proveedor nuevo(String nit, String nombre, String direccion, String telefono, String correo) {
        return new Proveedor(null, nit, nombre, direccion, telefono, correo, EstadoProveedor.ACTIVO);
    }

    public boolean estaActivo() {
        return estado == EstadoProveedor.ACTIVO;
    }

    public void actualizarDatos(String nombre, String direccion, String telefono, String correo) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
    }

    public void activar() {
        this.estado = EstadoProveedor.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoProveedor.INACTIVO;
    }

    public Long getId() {
        return id;
    }

    public String getNit() {
        return nit;
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

    public EstadoProveedor getEstado() {
        return estado;
    }
}
