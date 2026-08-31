package com.ais.marketbackend.fel.api.controllers;

import com.ais.marketbackend.fel.api.dtos.requests.AnularDocumentoFelRequest;
import com.ais.marketbackend.fel.api.dtos.responses.DocumentoFelResponse;
import com.ais.marketbackend.fel.api.mappers.DocumentoFelApiMapper;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
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

@RestController
@RequestMapping("/api/v1/fel/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class FelController {

    private final FelService felService;
    private final DocumentoFelApiMapper mapper;

    @GetMapping
    @RequiresPermission("FEL_VER")
    public ResponseEntity<PaginaResponse<DocumentoFelResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = felService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/{id}")
    @RequiresPermission("FEL_VER")
    public ResponseEntity<DocumentoFelResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(felService.obtener(tiendaId, id)));
    }

    @PostMapping("/ventas/{ventaId}/emitir")
    @RequiresPermission("FEL_EMITIR")
    public ResponseEntity<DocumentoFelResponse> emitir(@PathVariable Long tiendaId, @PathVariable Long ventaId) {
        return ResponseEntity.ok(mapper.toResponse(felService.emitir(tiendaId, ventaId)));
    }

    @PostMapping("/{id}/reintentar")
    @RequiresPermission("FEL_EMITIR")
    public ResponseEntity<DocumentoFelResponse> reintentar(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(felService.reintentar(tiendaId, id)));
    }

    @PostMapping("/{id}/anular")
    @RequiresPermission("FEL_ANULAR")
    public ResponseEntity<DocumentoFelResponse> anular(
            @PathVariable Long tiendaId, @PathVariable Long id, @Valid @RequestBody AnularDocumentoFelRequest request) {
        return ResponseEntity.ok(mapper.toResponse(felService.anular(tiendaId, id, request.motivo())));
    }
}
