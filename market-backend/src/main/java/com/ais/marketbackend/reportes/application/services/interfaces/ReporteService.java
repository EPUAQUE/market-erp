package com.ais.marketbackend.reportes.application.services.interfaces;

import com.ais.marketbackend.reportes.application.dtos.ReporteComprasResumen;
import com.ais.marketbackend.reportes.application.dtos.ReporteVentasResumen;
import java.time.Instant;

public interface ReporteService {

    ReporteVentasResumen reporteVentas(Long tiendaId, Instant desde, Instant hasta);

    ReporteComprasResumen reporteCompras(Long tiendaId, Instant desde, Instant hasta);
}
