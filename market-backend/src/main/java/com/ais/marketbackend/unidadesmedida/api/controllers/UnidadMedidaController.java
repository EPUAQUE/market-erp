package com.ais.marketbackend.unidadesmedida.api.controllers;

import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.unidadesmedida.api.dtos.requests.ActualizarUnidadMedidaRequest;
import com.ais.marketbackend.unidadesmedida.api.dtos.requests.CrearUnidadMedidaRequest;
import com.ais.marketbackend.unidadesmedida.api.dtos.responses.UnidadMedidaResponse;
import com.ais.marketbackend.unidadesmedida.api.mappers.UnidadMedidaApiMapper;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
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
@RequestMapping("/api/v1/unidades-medida")
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;
    private final UnidadMedidaApiMapper mapper;

    @GetMapping
    @RequiresPermission("UNIDADES_MEDIDA_VER")
    public ResponseEntity<List<UnidadMedidaResponse>> listar() {
        return ResponseEntity.ok(unidadMedidaService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("UNIDADES_MEDIDA_CREAR")
    public ResponseEntity<UnidadMedidaResponse> crear(@Valid @RequestBody CrearUnidadMedidaRequest request) {
        UnidadMedidaResponse creada = mapper.toResponse(
                unidadMedidaService.crear(request.nombre(), request.abreviacion()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @RequiresPermission("UNIDADES_MEDIDA_EDITAR")
    public ResponseEntity<UnidadMedidaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarUnidadMedidaRequest request) {
        UnidadMedidaResponse actualizada = mapper.toResponse(
                unidadMedidaService.actualizar(id, request.nombre(), request.abreviacion()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("UNIDADES_MEDIDA_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        unidadMedidaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("UNIDADES_MEDIDA_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        unidadMedidaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
