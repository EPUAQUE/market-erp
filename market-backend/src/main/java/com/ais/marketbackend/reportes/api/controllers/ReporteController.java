package com.ais.marketbackend.reportes.api.controllers;

import com.ais.marketbackend.reportes.api.dtos.responses.ReporteComprasResponse;
import com.ais.marketbackend.reportes.api.dtos.responses.ReporteVentasResponse;
import com.ais.marketbackend.reportes.api.mappers.ReporteApiMapper;
import com.ais.marketbackend.reportes.application.services.interfaces.ReporteService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteApiMapper mapper;

    @GetMapping("/ventas")
    @RequiresPermission("REPORTES_VER")
    public ResponseEntity<ReporteVentasResponse> reporteVentas(
            @PathVariable Long tiendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return ResponseEntity.ok(mapper.toResponse(reporteService.reporteVentas(tiendaId, desde, hasta)));
    }

    @GetMapping("/compras")
    @RequiresPermission("REPORTES_VER")
    public ResponseEntity<ReporteComprasResponse> reporteCompras(
            @PathVariable Long tiendaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
        return ResponseEntity.ok(mapper.toResponse(reporteService.reporteCompras(tiendaId, desde, hasta)));
    }
}
