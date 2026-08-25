package com.ais.marketbackend.inventario.api.controllers;

import com.ais.marketbackend.inventario.api.dtos.requests.RegistrarMovimientoRequest;
import com.ais.marketbackend.inventario.api.dtos.responses.InventarioResponse;
import com.ais.marketbackend.inventario.api.dtos.responses.MovimientoInventarioResponse;
import com.ais.marketbackend.inventario.api.mappers.InventarioApiMapper;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code tiendaId} va en la ruta (no en el body) precisamente para que
 * {@code PermissionInterceptor} aplique el alcance de tienda automáticamente —
 * a diferencia de {@code ProductoTiendaController}, donde no era posible.
 */
@RestController
@RequestMapping("/api/v1/inventario/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final InventarioApiMapper mapper;

    @GetMapping
    @RequiresPermission("INVENTARIO_VER")
    public ResponseEntity<PaginaResponse<InventarioResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = inventarioService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/productos/{productoId}")
    @RequiresPermission("INVENTARIO_VER")
    public ResponseEntity<InventarioResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long productoId) {
        return ResponseEntity.ok(mapper.toResponse(inventarioService.obtener(tiendaId, productoId)));
    }

    @GetMapping("/productos/{productoId}/movimientos")
    @RequiresPermission("INVENTARIO_VER")
    public ResponseEntity<PaginaResponse<MovimientoInventarioResponse>> listarMovimientos(
            @PathVariable Long tiendaId, @PathVariable Long productoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = inventarioService.listarMovimientos(
                tiendaId, productoId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @PostMapping("/movimientos")
    @RequiresPermission("INVENTARIO_AJUSTAR")
    public ResponseEntity<InventarioResponse> registrarMovimiento(
            @PathVariable Long tiendaId, @Valid @RequestBody RegistrarMovimientoRequest request) {
        InventarioResponse actualizado = mapper.toResponse(inventarioService.registrarMovimiento(
                tiendaId, request.productoId(), request.cantidad(), request.costoUnitario(),
                request.tipoMovimiento()));
        return ResponseEntity.status(HttpStatus.CREATED).body(actualizado);
    }
}
