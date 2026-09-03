package com.ais.marketbackend.marcas.api.controllers;

import com.ais.marketbackend.marcas.api.dtos.requests.ActualizarMarcaRequest;
import com.ais.marketbackend.marcas.api.dtos.requests.CrearMarcaRequest;
import com.ais.marketbackend.marcas.api.dtos.responses.MarcaResponse;
import com.ais.marketbackend.marcas.api.mappers.MarcaApiMapper;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
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
@RequestMapping("/api/v1/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;
    private final MarcaApiMapper mapper;

    @GetMapping
    @RequiresPermission("MARCAS_VER")
    public ResponseEntity<List<MarcaResponse>> listar() {
        return ResponseEntity.ok(marcaService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("MARCAS_CREAR")
    public ResponseEntity<MarcaResponse> crear(@Valid @RequestBody CrearMarcaRequest request) {
        MarcaResponse creada = mapper.toResponse(marcaService.crear(request.nombre()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @RequiresPermission("MARCAS_EDITAR")
    public ResponseEntity<MarcaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarMarcaRequest request) {
        MarcaResponse actualizada = mapper.toResponse(marcaService.actualizar(id, request.nombre()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("MARCAS_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        marcaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("MARCAS_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        marcaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
