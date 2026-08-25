package com.ais.marketbackend.seguridad.domain.model;

import java.util.Objects;

/**
 * Agregado raíz de identidad. No conoce JPA ni Spring Security: solo las reglas
 * de negocio de una cuenta (estado, versión de seguridad, cambio de contraseña).
 */
public class Usuario {

    private final Long id;
    private final String username;
    private String passwordHash;
    private EstadoUsuario estado;
    private long versionSeguridad;

    public Usuario(Long id, String username, String passwordHash, EstadoUsuario estado, long versionSeguridad) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.versionSeguridad = versionSeguridad;
    }

    public static Usuario nuevo(String username, String passwordHash) {
        return new Usuario(null, username, passwordHash, EstadoUsuario.ACTIVO, 0L);
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash, "nuevoHash");
        incrementarVersionSeguridad();
    }

    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
        incrementarVersionSeguridad();
    }

    public void bloquear() {
        this.estado = EstadoUsuario.BLOQUEADO;
        incrementarVersionSeguridad();
    }

    public void activar() {
        this.estado = EstadoUsuario.ACTIVO;
        incrementarVersionSeguridad();
    }

    private void incrementarVersionSeguridad() {
        this.versionSeguridad++;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public long getVersionSeguridad() {
        return versionSeguridad;
    }
}
