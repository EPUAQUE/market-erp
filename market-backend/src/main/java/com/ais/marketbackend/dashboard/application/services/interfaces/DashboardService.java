package com.ais.marketbackend.dashboard.application.services.interfaces;

import com.ais.marketbackend.dashboard.application.dtos.DashboardGrupoResumen;
import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;

public interface DashboardService {

    DashboardResumen obtenerResumen(Long tiendaId);

    /** Agrega {@link #obtenerResumen(Long)} de cada tienda del grupo. Exige acceso al grupo. */
    DashboardGrupoResumen obtenerResumenGrupo(Long grupoId);
}
