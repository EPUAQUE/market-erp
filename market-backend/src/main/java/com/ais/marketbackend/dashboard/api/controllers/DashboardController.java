package com.ais.marketbackend.dashboard.api.controllers;

import com.ais.marketbackend.dashboard.api.dtos.responses.DashboardResponse;
import com.ais.marketbackend.dashboard.api.mappers.DashboardApiMapper;
import com.ais.marketbackend.dashboard.application.services.interfaces.DashboardService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardApiMapper mapper;
    private final UsuarioService usuarioService;

    @GetMapping
    @RequiresPermission("DASHBOARD_VER")
    public ResponseEntity<DashboardResponse> obtenerResumen(@PathVariable Long tiendaId, Authentication authentication) {
        boolean verFinanciero = usuarioService
                .obtenerPermisosEfectivosPorUsername(authentication.getName())
                .tienePermiso("DASHBOARD_FINANCIERO_VER");
        return ResponseEntity.ok(mapper.toResponse(dashboardService.obtenerResumen(tiendaId), verFinanciero));
    }
}
