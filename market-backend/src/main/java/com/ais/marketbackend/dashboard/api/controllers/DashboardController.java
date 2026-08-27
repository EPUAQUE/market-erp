package com.ais.marketbackend.dashboard.api.controllers;

import com.ais.marketbackend.dashboard.api.dtos.responses.DashboardGrupoResponse;
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
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardApiMapper mapper;
    private final UsuarioService usuarioService;

    @GetMapping("/api/v1/dashboard/tiendas/{tiendaId}")
    @RequiresPermission("DASHBOARD_VER")
    public ResponseEntity<DashboardResponse> obtenerResumen(@PathVariable Long tiendaId, Authentication authentication) {
        boolean verFinanciero = tienePermisoFinanciero(authentication);
        return ResponseEntity.ok(mapper.toResponse(dashboardService.obtenerResumen(tiendaId), verFinanciero));
    }

    /**
     * Sin {@code {tiendaId}} en la ruta: {@code PermissionInterceptor} no aplica el
     * chequeo automático de alcance de tienda. La validación de alcance de grupo se
     * hace explícita dentro de {@code DashboardServiceImpl}, mismo patrón que
     * {@code TrasladoServiceImpl} usa para sus operaciones sin tienda única de ruta.
     */
    @GetMapping("/api/v1/dashboard/grupos/{grupoId}")
    @RequiresPermission("DASHBOARD_VER")
    public ResponseEntity<DashboardGrupoResponse> obtenerResumenGrupo(
            @PathVariable Long grupoId, Authentication authentication) {
        boolean verFinanciero = tienePermisoFinanciero(authentication);
        return ResponseEntity.ok(mapper.toResponseGrupo(dashboardService.obtenerResumenGrupo(grupoId), verFinanciero));
    }

    private boolean tienePermisoFinanciero(Authentication authentication) {
        return usuarioService
                .obtenerPermisosEfectivosPorUsername(authentication.getName())
                .tienePermiso("DASHBOARD_FINANCIERO_VER");
    }
}
