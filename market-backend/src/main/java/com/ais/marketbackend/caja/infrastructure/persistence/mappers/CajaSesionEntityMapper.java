package com.ais.marketbackend.caja.infrastructure.persistence.mappers;

import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.caja.domain.model.MovimientoCaja;
import com.ais.marketbackend.caja.infrastructure.persistence.entities.CajaSesionEntity;
import com.ais.marketbackend.caja.infrastructure.persistence.entities.MovimientoCajaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code movimientos} necesita reconstruir la
 * referencia bidireccional {@code MovimientoCajaEntity.sesion}, igual que
 * {@code CompraEntityMapper}.
 */
@Component
public class CajaSesionEntityMapper {

    public CajaSesion toDomain(CajaSesionEntity entity) {
        List<MovimientoCaja> movimientos = entity.getMovimientos().stream()
                .map(m -> new MovimientoCaja(
                        m.getId(), m.getFecha(), m.getTipo(), m.getConcepto(), m.getMonto(), m.getCorrelationId()))
                .toList();
        return new CajaSesion(
                entity.getId(), entity.getTiendaId(), entity.getFechaApertura(), entity.getFechaCierre(),
                entity.getMontoInicial(), entity.getMontoFinalContado(), entity.getEstado(), movimientos,
                entity.getCorrelationIdApertura(), entity.getCorrelationIdCierre());
    }

    public CajaSesionEntity toEntity(CajaSesion domain) {
        CajaSesionEntity entity = new CajaSesionEntity(
                domain.getId(), domain.getTiendaId(), domain.getFechaApertura(), domain.getFechaCierre(),
                domain.getMontoInicial(), domain.getMontoFinalContado(), domain.getEstado(),
                domain.getCorrelationIdApertura(), domain.getCorrelationIdCierre());
        List<MovimientoCajaEntity> movimientos = domain.getMovimientos().stream()
                .map(m -> new MovimientoCajaEntity(
                        m.getId(), entity, m.getFecha(), m.getTipo(), m.getConcepto(), m.getMonto(),
                        m.getCorrelationId()))
                .toList();
        entity.getMovimientos().addAll(movimientos);
        return entity;
    }
}
