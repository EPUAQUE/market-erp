package com.ais.marketbackend.compras.api.controllers;

import com.ais.marketbackend.compras.api.dtos.requests.CrearCompraRequest;
import com.ais.marketbackend.compras.api.dtos.responses.CompraResponse;
import com.ais.marketbackend.compras.api.mappers.CompraApiMapper;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/compras/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;
    private final CompraApiMapper mapper;

    @GetMapping
    @RequiresPermission("COMPRAS_VER")
    public ResponseEntity<PaginaResponse<CompraResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = compraService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("COMPRAS_VER")
    public ResponseEntity<CompraResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(compraService.obtener(tiendaId, id)));
    }

    @PostMapping
    @RequiresPermission("COMPRAS_CREAR")
    public ResponseEntity<CompraResponse> crear(
            @PathVariable Long tiendaId, @Valid @RequestBody CrearCompraRequest request) {
        List<NuevaLineaCompra> lineas = request.lineas().stream()
                .map(l -> new NuevaLineaCompra(l.productoId(), l.cantidad(), l.costoUnitario()))
                .toList();
        CompraResponse creada = mapper.toResponse(compraService.crear(tiendaId, request.proveedorId(), lineas));
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{id}/recibir")
    @RequiresPermission("COMPRAS_RECIBIR")
    public ResponseEntity<CompraResponse> recibir(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(compraService.recibir(tiendaId, id)));
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("COMPRAS_ANULAR")
    public ResponseEntity<CompraResponse> anular(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(compraService.anular(tiendaId, id)));
    }
}
