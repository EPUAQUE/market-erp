package com.ais.marketbackend.grupostienda.api.controllers;

import com.ais.marketbackend.grupostienda.api.dtos.requests.ActualizarGrupoTiendaRequest;
import com.ais.marketbackend.grupostienda.api.dtos.requests.CrearGrupoTiendaRequest;
import com.ais.marketbackend.grupostienda.api.dtos.responses.GrupoTiendaResponse;
import com.ais.marketbackend.grupostienda.api.mappers.GrupoTiendaApiMapper;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/grupos-tienda")
@RequiredArgsConstructor
public class GrupoTiendaController {

    private final GrupoTiendaService grupoTiendaService;
    private final GrupoTiendaApiMapper mapper;

    @GetMapping
    @RequiresPermission("GRUPOS_TIENDA_VER")
    public ResponseEntity<List<GrupoTiendaResponse>> listar() {
        return ResponseEntity.ok(grupoTiendaService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("GRUPOS_TIENDA_CREAR")
    public ResponseEntity<GrupoTiendaResponse> crear(@Valid @RequestBody CrearGrupoTiendaRequest request) {
        GrupoTiendaResponse creado =
                mapper.toResponse(grupoTiendaService.crear(request.codigo(), request.nombre()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @RequiresPermission("GRUPOS_TIENDA_EDITAR")
    public ResponseEntity<GrupoTiendaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarGrupoTiendaRequest request) {
        GrupoTiendaResponse actualizado = mapper.toResponse(grupoTiendaService.actualizar(id, request.nombre()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("GRUPOS_TIENDA_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        grupoTiendaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("GRUPOS_TIENDA_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        grupoTiendaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
