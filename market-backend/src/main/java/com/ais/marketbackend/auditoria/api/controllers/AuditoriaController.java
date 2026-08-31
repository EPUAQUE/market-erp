package com.ais.marketbackend.auditoria.api.controllers;

import com.ais.marketbackend.auditoria.api.dtos.responses.AuditEventResponse;
import com.ais.marketbackend.auditoria.api.mappers.AuditEventApiMapper;
import com.ais.marketbackend.auditoria.application.services.interfaces.AuditoriaService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura únicamente — escribir en {@code audit_event} pasa por
 * {@code AuditoriaRegistrador} desde dentro del backend, nunca por un endpoint HTTP
 * (no tendría sentido que un cliente pudiera insertar sus propias filas de
 * auditoría). Sin {@code tiendaId} en la ruta base a propósito: hay eventos sin
 * tienda (login fallido, rate limit) que un endpoint tienda-scoped dejaría fuera.
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final AuditEventApiMapper mapper;

    @GetMapping
    @RequiresPermission("AUDITORIA_VER")
    public ResponseEntity<PaginaResponse<AuditEventResponse>> listarTodo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = auditoriaService.listarTodo(
                PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @GetMapping("/tiendas/{tiendaId}")
    @RequiresPermission("AUDITORIA_VER")
    public ResponseEntity<PaginaResponse<AuditEventResponse>> listarPorTienda(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = auditoriaService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }
}
