package com.ais.marketbackend.seguridad.api.controllers;

import com.ais.marketbackend.seguridad.api.dtos.requests.AsignarTiendaRolRequest;
import com.ais.marketbackend.seguridad.api.dtos.requests.CrearUsuarioRequest;
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
}
