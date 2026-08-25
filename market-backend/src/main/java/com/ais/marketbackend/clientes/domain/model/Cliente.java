package com.ais.marketbackend.clientes.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Agregado raíz de un cliente. A diferencia de {@code Proveedor}, {@code nit} es
 * opcional: una venta a Consumidor Final (CF) no requiere NIT real para la
 * facturación electrónica (FEL Guatemala) — ver ARCHITECTURE.md. Cuando no es
 * nulo, sigue siendo el identificador fiscal inmutable.
 *
 * <p>{@code limiteCredito} es opcional (`null` = sin límite definido/evaluado
 * todavía) — no todo cliente compra a crédito, y no se le asigna un límite
 * por defecto solo por existir. Nada en este módulo valida ventas a crédito
 * contra este límite: eso vive del lado de quien decide otorgar el crédito
 * (hoy, el vendedor en el POS), este campo solo lo expone para consulta.
 */
public class Cliente {

    private final Long id;
    private final String nit;
    private String nombre;
    private String direccion;
    private String telefono;
    private String correo;
    private EstadoCliente estado;
    private BigDecimal limiteCredito;

    public Cliente(
            Long id, String nit, String nombre, String direccion, String telefono, String correo,
            EstadoCliente estado, BigDecimal limiteCredito) {
        this.id = id;
        this.nit = nit;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.limiteCredito = limiteCredito;
    }

    public static Cliente nuevo(
            String nit, String nombre, String direccion, String telefono, String correo,
            BigDecimal limiteCredito) {
        return new Cliente(null, nit, nombre, direccion, telefono, correo, EstadoCliente.ACTIVO, limiteCredito);
    }

    public boolean estaActivo() {
        return estado == EstadoCliente.ACTIVO;
    }

    public void actualizarDatos(
            String nombre, String direccion, String telefono, String correo, BigDecimal limiteCredito) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.direccion = direccion;
        this.telefono = telefono;
        this.correo = correo;
        this.limiteCredito = limiteCredito;
    }

    public void activar() {
        this.estado = EstadoCliente.ACTIVO;
    }

    public void desactivar() {
        this.estado = EstadoCliente.INACTIVO;
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

    public EstadoCliente getEstado() {
        return estado;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }
}
