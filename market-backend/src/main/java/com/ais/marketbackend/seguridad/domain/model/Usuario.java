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
    private String nombre;
    private String telefono;
    private String correo;
    private boolean debeCambiarPassword;

    public Usuario(
            Long id, String username, String passwordHash, EstadoUsuario estado, long versionSeguridad,
            String nombre, String telefono, String correo, boolean debeCambiarPassword) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.versionSeguridad = versionSeguridad;
        // Sin requireNonNull a propósito: obligatorios para altas nuevas (ver
        // CrearUsuarioRequest, @NotBlank), pero la columna es nullable/sin backfill
        // (ver seguridad/009-usuario-datos-personales.xml) — filas sembradas antes de
        // este cambio (AdminUserSeeder, seeds de prueba) no tienen estos datos.
        this.nombre = nombre;
        this.telefono = telefono;
        this.correo = correo;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    /** Conveniencia para pruebas que no necesitan los datos de perfil (login/password). */
    public static Usuario nuevo(String username, String passwordHash) {
        return nuevo(username, passwordHash, null, null, null);
    }

    public static Usuario nuevo(String username, String passwordHash, String nombre, String telefono, String correo) {
        return new Usuario(null, username, passwordHash, EstadoUsuario.ACTIVO, 0L, nombre, telefono, correo, false);
    }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    /** Cambio normal (autoservicio o restablecimiento administrativo): siempre limpia la marca de "debe cambiar". */
    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash, "nuevoHash");
        this.debeCambiarPassword = false;
        incrementarVersionSeguridad();
    }

    /**
     * Usado por el restablecimiento administrativo con contraseña temporal: aplica el
     * nuevo hash generado por el sistema y marca la cuenta para forzar el cambio en el
     * próximo login — a diferencia de {@link #cambiarPassword}, que se usa cuando el
     * cambio ya es definitivo (autoservicio, o un admin fijando la contraseña final).
     */
    public void restablecerConPasswordTemporal(String nuevoHash) {
        this.passwordHash = Objects.requireNonNull(nuevoHash, "nuevoHash");
        this.debeCambiarPassword = true;
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

    /**
     * Invalida de inmediato todos los access tokens ya emitidos (ver
     * {@code SecurityVersionValidator}) y deja sin efecto todos los refresh
     * tokens vigentes, sin tocar contraseña ni estado — para cuando se sospecha
     * de una sesión comprometida pero la cuenta y su contraseña siguen siendo
     * válidas.
     */
    public void revocarSesiones() {
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

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }
}
