package com.ais.marketbackend.seguridad.api.controllers;

import com.ais.marketbackend.seguridad.api.dtos.requests.AsignarGrupoRolRequest;
import com.ais.marketbackend.seguridad.api.dtos.requests.AsignarTiendaRolRequest;
import com.ais.marketbackend.seguridad.api.dtos.requests.CrearUsuarioRequest;
import com.ais.marketbackend.seguridad.api.dtos.responses.RestablecerPasswordResponse;
import com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioGrupoTiendaResponse;
import com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioResponse;
import com.ais.marketbackend.seguridad.api.dtos.responses.UsuarioTiendaResponse;
import com.ais.marketbackend.seguridad.api.mappers.UsuarioApiMapper;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioApiMapper mapper;

    @GetMapping
    @RequiresPermission("USUARIOS_VER")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        List<UsuarioResponse> usuarios = usuarioService.listar().stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping
    @RequiresPermission("USUARIOS_CREAR")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse creado = mapper.toResponse(usuarioService.crear(
                request.username(), request.password(), request.nombre(), request.telefono(), request.correo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/{usuarioId}/tiendas")
    @RequiresPermission("USUARIOS_ASIGNAR_TIENDA")
    public ResponseEntity<Void> asignarTienda(
            @PathVariable Long usuarioId, @Valid @RequestBody AsignarTiendaRolRequest request) {
        usuarioService.asignarTienda(usuarioId, request.tiendaId(), request.rolId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{usuarioId}/tiendas")
    @RequiresPermission("USUARIOS_VER")
    public ResponseEntity<List<UsuarioTiendaResponse>> listarTiendas(@PathVariable Long usuarioId) {
        List<UsuarioTiendaResponse> asignaciones = usuarioService.listarTiendas(usuarioId).stream()
                .map(ut -> new UsuarioTiendaResponse(ut.id(), ut.tiendaId(), ut.rolId(), ut.rolNombre()))
                .toList();
        return ResponseEntity.ok(asignaciones);
    }

    @PostMapping("/{usuarioId}/grupos")
    @RequiresPermission("USUARIOS_ASIGNAR_GRUPO")
    public ResponseEntity<Void> asignarGrupo(
            @PathVariable Long usuarioId, @Valid @RequestBody AsignarGrupoRolRequest request) {
        usuarioService.asignarGrupo(usuarioId, request.grupoTiendaId(), request.rolId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{usuarioId}/grupos")
    @RequiresPermission("USUARIOS_VER")
    public ResponseEntity<List<UsuarioGrupoTiendaResponse>> listarGrupos(@PathVariable Long usuarioId) {
        List<UsuarioGrupoTiendaResponse> asignaciones = usuarioService.listarGrupos(usuarioId).stream()
                .map(ug -> new UsuarioGrupoTiendaResponse(ug.id(), ug.grupoTiendaId(), ug.rolId(), ug.rolNombre()))
                .toList();
        return ResponseEntity.ok(asignaciones);
    }

    /**
     * Acción sobre OTRO usuario — a diferencia de {@code AuthController.cambiarMiPassword}
     * (autoservicio), esta exige un permiso administrativo dedicado.
     */
    @PostMapping("/{usuarioId}/password/restablecer")
    @RequiresPermission("USUARIOS_RESTABLECER_PASSWORD")
    public ResponseEntity<RestablecerPasswordResponse> restablecerPassword(@PathVariable Long usuarioId) {
        String passwordTemporal = usuarioService.restablecerPassword(usuarioId);
        return ResponseEntity.ok(RestablecerPasswordResponse.builder().passwordTemporal(passwordTemporal).build());
    }

    /**
     * Revoca todas las sesiones activas de OTRO usuario (refresh tokens + access
     * tokens ya emitidos, vía versión de seguridad) sin cambiar su contraseña ni
     * su estado — p. ej. ante sospecha de sesión comprometida.
     */
    @PostMapping("/{usuarioId}/sesiones/revocar")
    @RequiresPermission("USUARIOS_REVOCAR_SESIONES")
    public ResponseEntity<Void> revocarSesiones(@PathVariable Long usuarioId) {
        usuarioService.revocarSesiones(usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Baja de empleado (Fase 4, PLAN_MEJORAS.md): el dominio ya tenía
     * {@code Usuario.desactivar()} desde antes de esta fase pero nunca estuvo
     * expuesto por HTTP — no había forma real de dar de baja a alguien vía la API.
     * No hay protección contra que un admin se desactive a sí mismo (ver
     * seguridad-desarrolladores.md §4, "Baja de empleados") — usar con cuidado.
     */
    @PostMapping("/{usuarioId}/desactivar")
    @RequiresPermission("USUARIOS_CAMBIAR_ESTADO")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(mapper.toResponse(usuarioService.desactivar(usuarioId)));
    }

    /** Mismo caso que {@link #desactivar}, motivo distinto (sospecha de seguridad, no cese normal). */
    @PostMapping("/{usuarioId}/bloquear")
    @RequiresPermission("USUARIOS_CAMBIAR_ESTADO")
    public ResponseEntity<UsuarioResponse> bloquear(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(mapper.toResponse(usuarioService.bloquear(usuarioId)));
    }

    @PostMapping("/{usuarioId}/activar")
    @RequiresPermission("USUARIOS_CAMBIAR_ESTADO")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(mapper.toResponse(usuarioService.activar(usuarioId)));
    }
}
