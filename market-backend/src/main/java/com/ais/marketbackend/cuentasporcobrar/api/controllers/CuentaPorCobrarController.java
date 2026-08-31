package com.ais.marketbackend.cuentasporcobrar.api.controllers;

import com.ais.marketbackend.cuentasporcobrar.api.dtos.requests.RegistrarCobroRequest;
import com.ais.marketbackend.cuentasporcobrar.api.dtos.responses.CuentaPorCobrarResponse;
import com.ais.marketbackend.cuentasporcobrar.api.mappers.CuentaPorCobrarApiMapper;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * No expone creación: una cuenta por cobrar nace únicamente al completar una
 * {@code Venta} (ver {@code VentaServiceImpl.completar}), nunca por acción
 * directa del usuario vía esta API.
 */
@RestController
@RequestMapping("/api/v1/cuentas-por-cobrar/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class CuentaPorCobrarController {

    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorCobrarApiMapper mapper;

    @GetMapping
    @RequiresPermission("CUENTAS_POR_COBRAR_VER")
    public ResponseEntity<PaginaResponse<CuentaPorCobrarResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = cuentaPorCobrarService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("CUENTAS_POR_COBRAR_VER")
    public ResponseEntity<CuentaPorCobrarResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(cuentaPorCobrarService.obtener(tiendaId, id)));
    }

    /** Fase 11 (PLAN_MEJORAS.md): 404 si la venta no tiene cuenta por cobrar (ej. venta al contado) — caso normal. */
    @GetMapping("/por-venta/{ventaId}")
    @RequiresPermission("CUENTAS_POR_COBRAR_VER")
    public ResponseEntity<CuentaPorCobrarResponse> obtenerPorVenta(
            @PathVariable Long tiendaId, @PathVariable Long ventaId) {
        return ResponseEntity.ok(mapper.toResponse(cuentaPorCobrarService.obtenerPorVenta(tiendaId, ventaId)));
    }

    @PostMapping("/{id}/cobros")
    @RequiresPermission("CUENTAS_POR_COBRAR_COBRAR")
    public ResponseEntity<CuentaPorCobrarResponse> registrarCobro(
            @PathVariable Long tiendaId, @PathVariable Long id, @Valid @RequestBody RegistrarCobroRequest request) {
        CuentaPorCobrarResponse actualizada = mapper.toResponse(
                cuentaPorCobrarService.registrarCobro(tiendaId, id, request.monto(), request.metodoPago()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("CUENTAS_POR_COBRAR_ANULAR")
    public ResponseEntity<CuentaPorCobrarResponse> anular(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(cuentaPorCobrarService.anular(tiendaId, id)));
    }
}
