package com.ais.marketbackend.productos.api.controllers;

import com.ais.marketbackend.productos.api.dtos.requests.ActualizarProductoTiendaRequest;
import com.ais.marketbackend.productos.api.dtos.requests.AsignarProductoTiendaRequest;
import com.ais.marketbackend.productos.api.dtos.responses.ProductoTiendaResponse;
import com.ais.marketbackend.productos.api.mappers.ProductoTiendaApiMapper;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
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

/**
 * Configuración de un producto por tienda (precio, stock, permitirVenta/Ingreso).
 * {@code tiendaId} llega en el cuerpo o se resuelve desde la entidad (no en la
 * ruta), así que el alcance por tienda del {@code PermissionInterceptor} no
 * aplica aquí — {@code ProductoTiendaServiceImpl} valida el alcance
 * explícitamente vía {@code AutorizacionTiendaService} en cada operación.
 */
@RestController
@RequestMapping("/api/v1/productos/{productoId}/tiendas")
@RequiredArgsConstructor
public class ProductoTiendaController {

    private final ProductoTiendaService productoTiendaService;
    private final ProductoTiendaApiMapper mapper;

    @GetMapping
    @RequiresPermission("PRODUCTOS_VER")
    public ResponseEntity<List<ProductoTiendaResponse>> listar(@PathVariable Long productoId) {
        List<ProductoTiendaResponse> items = productoTiendaService.listarPorProducto(productoId).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<ProductoTiendaResponse> asignar(
            @PathVariable Long productoId, @Valid @RequestBody AsignarProductoTiendaRequest request) {
        ProductoTiendaResponse creado = mapper.toResponse(productoTiendaService.asignar(
                productoId, request.tiendaId(), request.precioVenta(), request.stockMinimo(),
                request.stockMaximo(), request.permitirVenta(), request.permitirIngreso()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<ProductoTiendaResponse> actualizar(
            @PathVariable Long productoId, @PathVariable Long id,
            @Valid @RequestBody ActualizarProductoTiendaRequest request) {
        ProductoTiendaResponse actualizado = mapper.toResponse(productoTiendaService.actualizar(
                id, request.precioVenta(), request.stockMinimo(), request.stockMaximo(), request.permitirVenta(),
                request.permitirIngreso()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long productoId, @PathVariable Long id) {
        productoTiendaService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long productoId, @PathVariable Long id) {
        productoTiendaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
