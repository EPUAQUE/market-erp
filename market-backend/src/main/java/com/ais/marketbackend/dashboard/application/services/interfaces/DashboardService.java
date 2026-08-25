package com.ais.marketbackend.dashboard.application.services.interfaces;

import com.ais.marketbackend.dashboard.application.dtos.DashboardResumen;

public interface DashboardService {

    DashboardResumen obtenerResumen(Long tiendaId);
}
