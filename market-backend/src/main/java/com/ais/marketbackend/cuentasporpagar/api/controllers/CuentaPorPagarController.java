package com.ais.marketbackend.cuentasporpagar.api.controllers;

import com.ais.marketbackend.cuentasporpagar.api.dtos.requests.RegistrarPagoRequest;
import com.ais.marketbackend.cuentasporpagar.api.dtos.responses.CuentaPorPagarResponse;
import com.ais.marketbackend.cuentasporpagar.api.mappers.CuentaPorPagarApiMapper;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
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
 * No expone creación: una cuenta por pagar nace únicamente al recibir una
 * {@code Compra} (ver {@code CompraServiceImpl.recibir}), nunca por acción
 * directa del usuario vía esta API.
 */
@RestController
@RequestMapping("/api/v1/cuentas-por-pagar/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class CuentaPorPagarController {

    private final CuentaPorPagarService cuentaPorPagarService;
    private final CuentaPorPagarApiMapper mapper;

    @GetMapping
    @RequiresPermission("CUENTAS_POR_PAGAR_VER")
    public ResponseEntity<PaginaResponse<CuentaPorPagarResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = cuentaPorPagarService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("CUENTAS_POR_PAGAR_VER")
    public ResponseEntity<CuentaPorPagarResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(cuentaPorPagarService.obtener(tiendaId, id)));
    }

    @PostMapping("/{id}/pagos")
    @RequiresPermission("CUENTAS_POR_PAGAR_PAGAR")
    public ResponseEntity<CuentaPorPagarResponse> registrarPago(
            @PathVariable Long tiendaId, @PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest request) {
        CuentaPorPagarResponse actualizada =
                mapper.toResponse(cuentaPorPagarService.registrarPago(tiendaId, id, request.monto()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("CUENTAS_POR_PAGAR_ANULAR")
    public ResponseEntity<CuentaPorPagarResponse> anular(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(cuentaPorPagarService.anular(tiendaId, id)));
    }
}
