package com.ais.marketbackend.seguridad.api.controllers;

import com.ais.marketbackend.seguridad.api.dtos.responses.RolResponse;
import com.ais.marketbackend.seguridad.application.services.interfaces.RolService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @GetMapping
    // Mismo permiso que dispara la asignación (USUARIOS_ASIGNAR_TIENDA) — este
    // listado solo existe para poblar ese selector, ver UsuariosView.vue.
    @RequiresPermission("USUARIOS_ASIGNAR_TIENDA")
    public ResponseEntity<List<RolResponse>> listar() {
        List<RolResponse> roles = rolService.listar().stream()
                .map(r -> new RolResponse(r.id(), r.nombre(), r.alcanceGlobal()))
                .toList();
        return ResponseEntity.ok(roles);
    }
}
