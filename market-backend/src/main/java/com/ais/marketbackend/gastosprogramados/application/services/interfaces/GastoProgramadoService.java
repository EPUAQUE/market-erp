package com.ais.marketbackend.gastosprogramados.application.services.interfaces;

import com.ais.marketbackend.gastosprogramados.application.dtos.GastoProgramadoResumen;
import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface GastoProgramadoService {

    GastoProgramadoResumen crear(
            Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia, Instant fechaInicio);

    GastoProgramadoResumen actualizar(
            Long tiendaId, Long id, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia);

    GastoProgramadoResumen activar(Long tiendaId, Long id);

    GastoProgramadoResumen desactivar(Long tiendaId, Long id);

    GastoProgramadoResumen generarPago(Long tiendaId, Long id);

    GastoProgramadoResumen obtener(Long tiendaId, Long id);

    List<GastoProgramadoResumen> listarPorTienda(Long tiendaId);
}
