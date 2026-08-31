package com.ais.marketbackend.seguridad.application.services.interfaces;

import com.ais.marketbackend.seguridad.application.dtos.UsuarioGrupoTiendaResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioTiendaResumen;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import java.util.List;

public interface UsuarioService {

    UsuarioResumen crear(String username, String passwordPlano, String nombre, String telefono, String correo);

    /** Usado por otros módulos (p. ej. Ventas) para resolver el usuario autenticado desde el JWT. */
    UsuarioResumen obtenerPorUsername(String username);

    /**
     * Exige que quien llama (el usuario autenticado actual) tenga acceso a
     * {@code tiendaId} y no pueda escalar a un rol de alcance global — pensado para
     * el flujo HTTP de un administrador asignando tienda a otro usuario. Nunca
     * llamar desde código sin una autenticación real en {@code SecurityContextHolder}
     * (p. ej. un {@code ApplicationRunner} de arranque): usar
     * {@link #asignarTiendaSistema(Long, Long, Long)} para eso.
     */
    void asignarTienda(Long usuarioId, Long tiendaId, Long rolId);

    /**
     * Misma operación que {@link #asignarTienda(Long, Long, Long)} pero SIN exigir
     * acceso del llamador ni bloquear escalación de alcance global — no hay llamador
     * que autorizar porque no es un usuario autenticado quien la invoca. Uso
     * exclusivo de bootstrap de sistema ({@code AdminUserSeeder}); nunca exponerla
     * vía un endpoint HTTP.
     */
    void asignarTiendaSistema(Long usuarioId, Long tiendaId, Long rolId);

    List<UsuarioTiendaResumen> listarTiendas(Long usuarioId);

    /**
     * Asigna un usuario a un grupo de tiendas completo. Rechaza con
     * {@link com.ais.marketbackend.seguridad.domain.exception.AsignacionMixtaNoPermitidaException}
     * si el usuario ya tiene una asignación de tienda individual dentro de ese grupo.
     */
    void asignarGrupo(Long usuarioId, Long grupoTiendaId, Long rolId);

    List<UsuarioGrupoTiendaResumen> listarGrupos(Long usuarioId);

    PermisosEfectivos obtenerPermisosEfectivos(Long usuarioId);

    PermisosEfectivos obtenerPermisosEfectivosPorUsername(String username);

    List<UsuarioResumen> listar();

    /**
     * Autoservicio: el usuario autenticado cambia su propia contraseña, verificando
     * la actual. Revoca todas sus sesiones activas (refresh tokens) — debe volver a
     * iniciar sesión en cualquier otro dispositivo.
     */
    void cambiarMiPassword(Long usuarioId, String passwordActual, String passwordNueva);

    /**
     * Restablecimiento administrativo: genera una contraseña temporal aleatoria, la
     * aplica y marca la cuenta para forzar el cambio en el próximo login (ver
     * {@code Usuario.restablecerConPasswordTemporal}). Revoca todas las sesiones
     * activas del usuario. Devuelve la contraseña en texto plano — la única vez que
     * existe fuera del hash — para que quien llama se la entregue al usuario por un
     * canal separado; nunca se persiste ni se registra en auditoría.
     */
    String restablecerPassword(Long usuarioId);

    /**
     * Revoca todas las sesiones activas de otro usuario: invalida de inmediato
     * todos sus access tokens ya emitidos (versión de seguridad) y revoca todos
     * sus refresh tokens — sin cambiar su contraseña ni su estado. Pensado para
     * sospecha de sesión comprometida sin necesidad de forzar un cambio de
     * contraseña.
     */
    void revocarSesiones(Long usuarioId);

    /**
     * Baja de empleado (Fase 4, PLAN_MEJORAS.md): marca la cuenta {@code INACTIVO}
     * y revoca todas sus sesiones activas — invalida de inmediato cualquier access
     * token ya emitido (versión de seguridad) y todos sus refresh tokens. A
     * diferencia de {@link #bloquear(Long)}, pensado para un cese normal (no una
     * sospecha de seguridad); reversible con {@link #activar(Long)}.
     */
    UsuarioResumen desactivar(Long usuarioId);

    /**
     * Bloqueo administrativo inmediato (sospecha de compromiso, incidente de
     * seguridad) — misma invalidación de sesiones que {@link #desactivar(Long)},
     * estado distinto ({@code BLOQUEADO}) para diferenciar el motivo en pantalla y
     * en auditoría. Reversible con {@link #activar(Long)}.
     */
    UsuarioResumen bloquear(Long usuarioId);

    /** Reactiva una cuenta {@code INACTIVO} o {@code BLOQUEADO} — vuelve a {@code ACTIVO}. */
    UsuarioResumen activar(Long usuarioId);
}
