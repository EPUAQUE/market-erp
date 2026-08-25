package com.ais.marketbackend.categorias.api.controllers;

import com.ais.marketbackend.categorias.api.dtos.requests.ActualizarCategoriaRequest;
import com.ais.marketbackend.categorias.api.dtos.requests.CrearCategoriaRequest;
import com.ais.marketbackend.categorias.api.dtos.responses.CategoriaResponse;
import com.ais.marketbackend.categorias.api.mappers.CategoriaApiMapper;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final CategoriaApiMapper mapper;

    @GetMapping
    @RequiresPermission("CATEGORIAS_VER")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(categoriaService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("CATEGORIAS_CREAR")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CrearCategoriaRequest request) {
        CategoriaResponse creada = mapper.toResponse(categoriaService.crear(request.nombre(), request.imagen()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @RequiresPermission("CATEGORIAS_EDITAR")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarCategoriaRequest request) {
        CategoriaResponse actualizada = mapper.toResponse(
                categoriaService.actualizar(id, request.nombre(), request.imagen()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("CATEGORIAS_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        categoriaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("CATEGORIAS_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        categoriaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
