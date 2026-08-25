package com.ais.marketbackend.traslados.api.controllers;

import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import com.ais.marketbackend.traslados.api.dtos.requests.CrearTrasladoRequest;
import com.ais.marketbackend.traslados.api.dtos.responses.TrasladoResponse;
import com.ais.marketbackend.traslados.api.mappers.TrasladoApiMapper;
import com.ais.marketbackend.traslados.application.dtos.NuevaLineaTraslado;
import com.ais.marketbackend.traslados.application.services.interfaces.TrasladoService;
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

/**
 * Un traslado involucra dos tiendas (origen y destino), no una — a diferencia
 * de Compras/Ventas no hay una única {@code tiendaId} de la ruta a la que
 * anclar el alcance de tienda del {@code PermissionInterceptor}. El alcance de
 * tienda (origen y destino) se valida explícitamente en
 * {@code TrasladoServiceImpl} vía {@code AutorizacionTiendaService} — necesario
 * porque {@code ENCARGADO_TIENDA} (alcance por tienda, no global) también tiene
 * los permisos de traslados.
 */
@RestController
@RequestMapping("/api/v1/traslados")
@RequiredArgsConstructor
public class TrasladoController {

    private final TrasladoService trasladoService;
    private final TrasladoApiMapper mapper;

    @GetMapping
    @RequiresPermission("TRASLADOS_VER")
    public ResponseEntity<PaginaResponse<TrasladoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = trasladoService.listar(
                PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("TRASLADOS_VER")
    public ResponseEntity<TrasladoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(trasladoService.obtener(id)));
    }

    @PostMapping
    @RequiresPermission("TRASLADOS_CREAR")
    public ResponseEntity<TrasladoResponse> crear(@Valid @RequestBody CrearTrasladoRequest request) {
        List<NuevaLineaTraslado> lineas = request.lineas().stream()
                .map(l -> new NuevaLineaTraslado(l.productoId(), l.cantidad()))
                .toList();
        TrasladoResponse creado = mapper.toResponse(
                trasladoService.crear(request.tiendaOrigenId(), request.tiendaDestinoId(), lineas));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PostMapping("/{id}/completar")
    @RequiresPermission("TRASLADOS_COMPLETAR")
    public ResponseEntity<TrasladoResponse> completar(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(trasladoService.completar(id)));
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("TRASLADOS_ANULAR")
    public ResponseEntity<TrasladoResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(trasladoService.anular(id)));
    }
}
