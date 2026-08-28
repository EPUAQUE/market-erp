package com.ais.marketbackend.caja.api.controllers;

import com.ais.marketbackend.caja.api.dtos.requests.AbrirCajaRequest;
import com.ais.marketbackend.caja.api.dtos.requests.CerrarCajaRequest;
import com.ais.marketbackend.caja.api.dtos.requests.RegistrarMovimientoCajaRequest;
import com.ais.marketbackend.caja.api.dtos.responses.CajaSesionResponse;
import com.ais.marketbackend.caja.api.mappers.CajaApiMapper;
import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
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

/** {@code tiendaId} va en la ruta para que {@code PermissionInterceptor} aplique el alcance de tienda. */
@RestController
@RequestMapping("/api/v1/caja/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;
    private final CajaApiMapper mapper;

    @GetMapping
    @RequiresPermission("CAJA_VER")
    public ResponseEntity<PaginaResponse<CajaSesionResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = cajaService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/abierta")
    @RequiresPermission("CAJA_VER")
    public ResponseEntity<CajaSesionResponse> obtenerAbierta(@PathVariable Long tiendaId) {
        return ResponseEntity.ok(mapper.toResponse(cajaService.obtenerAbierta(tiendaId)));
    }

    @GetMapping("/{id}")
    @RequiresPermission("CAJA_VER")
    public ResponseEntity<CajaSesionResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(cajaService.obtener(tiendaId, id)));
    }

    @PostMapping("/abrir")
    @RequiresPermission("CAJA_ABRIR")
    public ResponseEntity<CajaSesionResponse> abrir(
            @PathVariable Long tiendaId, @Valid @RequestBody AbrirCajaRequest request) {
        CajaSesionResponse creada =
                mapper.toResponse(cajaService.abrir(tiendaId, request.montoInicial(), request.correlationId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/movimientos")
    @RequiresPermission("CAJA_REGISTRAR_MOVIMIENTO")
    public ResponseEntity<CajaSesionResponse> registrarMovimiento(
            @PathVariable Long tiendaId, @Valid @RequestBody RegistrarMovimientoCajaRequest request) {
        CajaSesionResponse actualizada = mapper.toResponse(cajaService.registrarMovimiento(
                tiendaId, request.tipo(), request.concepto(), request.monto(), request.correlationId()));
        return ResponseEntity.ok(actualizada);
    }

    @PostMapping("/cerrar")
    @RequiresPermission("CAJA_CERRAR")
    public ResponseEntity<CajaSesionResponse> cerrar(
            @PathVariable Long tiendaId, @Valid @RequestBody CerrarCajaRequest request) {
        return ResponseEntity.ok(
                mapper.toResponse(cajaService.cerrar(tiendaId, request.montoFinalContado(), request.correlationId())));
    }
}
