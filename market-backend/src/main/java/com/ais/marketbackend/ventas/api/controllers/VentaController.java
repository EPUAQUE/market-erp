package com.ais.marketbackend.ventas.api.controllers;

import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import com.ais.marketbackend.ventas.api.dtos.requests.CompletarVentaRequest;
import com.ais.marketbackend.ventas.api.dtos.requests.CrearVentaRequest;
import com.ais.marketbackend.ventas.api.dtos.responses.VentaResponse;
import com.ais.marketbackend.ventas.api.mappers.VentaApiMapper;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.PagoInmediato;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code tiendaId} va en la ruta para que {@code PermissionInterceptor} aplique el alcance de tienda. */
@RestController
@RequestMapping("/api/v1/ventas/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;
    private final VentaApiMapper mapper;
    private final UsuarioService usuarioService;

    @GetMapping
    @RequiresPermission("VENTAS_VER")
    public ResponseEntity<PaginaResponse<VentaResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = ventaService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("VENTAS_VER")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(ventaService.obtener(tiendaId, id)));
    }

    @PostMapping
    @RequiresPermission("VENTAS_CREAR")
    public ResponseEntity<VentaResponse> crear(
            @PathVariable Long tiendaId, @Valid @RequestBody CrearVentaRequest request,
            Authentication authentication) {
        List<NuevaLineaVenta> lineas = request.lineas().stream()
                .map(l -> new NuevaLineaVenta(l.productoId(), l.cantidad(), l.precioUnitario()))
                .toList();
        Long vendedorId = usuarioService.obtenerPorUsername(authentication.getName()).id();
        VentaResponse creada = mapper.toResponse(ventaService.crear(
                tiendaId, request.clienteId(), vendedorId, lineas, request.metodoPago(), request.correlationId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /** {@code request}/{@code pagos} solo hace falta para una venta MIXTO — ver {@code CompletarVentaRequest}. */
    @PostMapping("/{id}/completar")
    @RequiresPermission("VENTAS_COMPLETAR")
    public ResponseEntity<VentaResponse> completar(
            @PathVariable Long tiendaId, @PathVariable Long id,
            @Valid @RequestBody(required = false) CompletarVentaRequest request) {
        List<PagoInmediato> pagos = request == null || request.pagos() == null
                ? List.of()
                : request.pagos().stream().map(p -> new PagoInmediato(p.metodoPago(), p.monto())).toList();
        return ResponseEntity.ok(mapper.toResponse(ventaService.completar(tiendaId, id, pagos)));
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("VENTAS_ANULAR")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(ventaService.anular(tiendaId, id)));
    }
}
