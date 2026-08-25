package com.ais.marketbackend.proveedores.api.controllers;

import com.ais.marketbackend.proveedores.api.dtos.requests.ActualizarProveedorRequest;
import com.ais.marketbackend.proveedores.api.dtos.requests.CrearProveedorRequest;
import com.ais.marketbackend.proveedores.api.dtos.responses.ProveedorResponse;
import com.ais.marketbackend.proveedores.api.mappers.ProveedorApiMapper;
import com.ais.marketbackend.proveedores.application.services.interfaces.ProveedorService;
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
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;
    private final ProveedorApiMapper mapper;

    @GetMapping
    @RequiresPermission("PROVEEDORES_VER")
    public ResponseEntity<List<ProveedorResponse>> listar() {
        return ResponseEntity.ok(proveedorService.listar().stream().map(mapper::toResponse).toList());
    }

    @PostMapping
    @RequiresPermission("PROVEEDORES_CREAR")
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody CrearProveedorRequest request) {
        ProveedorResponse creado = mapper.toResponse(proveedorService.crear(
                request.nit(), request.nombre(), request.direccion(), request.telefono(), request.correo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @RequiresPermission("PROVEEDORES_EDITAR")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarProveedorRequest request) {
        ProveedorResponse actualizado = mapper.toResponse(proveedorService.actualizar(
                id, request.nombre(), request.direccion(), request.telefono(), request.correo()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("PROVEEDORES_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        proveedorService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("PROVEEDORES_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
