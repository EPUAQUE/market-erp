package com.ais.marketbackend.tiendas.api.controllers;

import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.tiendas.api.dtos.requests.ActualizarTiendaRequest;
import com.ais.marketbackend.tiendas.api.dtos.requests.CrearTiendaRequest;
import com.ais.marketbackend.tiendas.api.dtos.responses.TiendaResponse;
import com.ais.marketbackend.tiendas.api.mappers.TiendaApiMapper;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
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
@RequestMapping("/api/v1/tiendas")
@RequiredArgsConstructor
public class TiendaController {

    private final TiendaService tiendaService;
    private final TiendaApiMapper mapper;

    @GetMapping
    @RequiresPermission("TIENDAS_VER")
    public ResponseEntity<List<TiendaResponse>> listar() {
        return ResponseEntity.ok(tiendaService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("TIENDAS_CREAR")
    public ResponseEntity<TiendaResponse> crear(@Valid @RequestBody CrearTiendaRequest request) {
        TiendaResponse creada = mapper.toResponse(tiendaService.crear(
                request.codigo(), request.nombre(), request.direccion(), request.telefono(), request.correo(),
                request.grupoId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @RequiresPermission("TIENDAS_EDITAR")
    public ResponseEntity<TiendaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarTiendaRequest request) {
        TiendaResponse actualizada = mapper.toResponse(tiendaService.actualizar(
                id, request.nombre(), request.direccion(), request.telefono(), request.correo(),
                request.grupoId()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("TIENDAS_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        tiendaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("TIENDAS_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        tiendaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
