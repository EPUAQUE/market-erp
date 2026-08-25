package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.mappers;

import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import com.ais.marketbackend.gastosprogramados.domain.model.PagoGastoProgramado;
import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities.GastoProgramadoEntity;
import com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities.PagoGastoProgramadoEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code pagos} necesita reconstruir la referencia
 * bidireccional {@code PagoGastoProgramadoEntity.gasto}.
 */
@Component
public class GastoProgramadoEntityMapper {

    public GastoProgramado toDomain(GastoProgramadoEntity entity) {
        List<PagoGastoProgramado> pagos = entity.getPagos().stream()
                .map(p -> new PagoGastoProgramado(p.getId(), p.getFecha(), p.getMonto()))
                .toList();
        return new GastoProgramado(
                entity.getId(), entity.getTiendaId(), entity.getConcepto(), entity.getMonto(),
                entity.getFrecuencia(), entity.getProximaFecha(), entity.isActivo(), pagos);
    }

    public GastoProgramadoEntity toEntity(GastoProgramado domain) {
        GastoProgramadoEntity entity = new GastoProgramadoEntity(
                domain.getId(), domain.getTiendaId(), domain.getConcepto(), domain.getMonto(),
                domain.getFrecuencia(), domain.getProximaFecha(), domain.isActivo());
        List<PagoGastoProgramadoEntity> pagos = domain.getPagos().stream()
                .map(p -> new PagoGastoProgramadoEntity(p.getId(), entity, p.getFecha(), p.getMonto()))
                .toList();
        entity.getPagos().addAll(pagos);
        return entity;
    }
}
