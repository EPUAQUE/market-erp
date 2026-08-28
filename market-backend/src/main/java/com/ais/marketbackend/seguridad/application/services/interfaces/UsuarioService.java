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
}
