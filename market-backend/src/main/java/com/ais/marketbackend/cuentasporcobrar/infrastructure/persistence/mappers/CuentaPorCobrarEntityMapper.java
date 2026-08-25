package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.mappers;

import com.ais.marketbackend.cuentasporcobrar.domain.model.Cobro;
import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities.CobroEntity;
import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities.CuentaPorCobrarEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code cobros} necesita reconstruir la referencia
 * bidireccional {@code CobroEntity.cuenta}, igual que {@code CuentaPorPagarEntityMapper}.
 */
@Component
public class CuentaPorCobrarEntityMapper {

    public CuentaPorCobrar toDomain(CuentaPorCobrarEntity entity) {
        List<Cobro> cobros = entity.getCobros().stream()
                .map(c -> new Cobro(c.getId(), c.getFecha(), c.getMonto(), c.getMetodoPago()))
                .toList();
        return new CuentaPorCobrar(
                entity.getId(), entity.getVentaId(), entity.getClienteId(), entity.getTiendaId(),
                entity.getFechaEmision(), entity.getFechaVencimiento(), entity.getMontoOriginal(),
                entity.getSaldoPendiente(), entity.getEstado(), cobros);
    }

    public CuentaPorCobrarEntity toEntity(CuentaPorCobrar domain) {
        CuentaPorCobrarEntity entity = new CuentaPorCobrarEntity(
                domain.getId(), domain.getVentaId(), domain.getClienteId(), domain.getTiendaId(),
                domain.getFechaEmision(), domain.getFechaVencimiento(), domain.getMontoOriginal(),
                domain.getSaldoPendiente(), domain.getEstado());
        List<CobroEntity> cobros = domain.getCobros().stream()
                .map(c -> new CobroEntity(c.getId(), entity, c.getFecha(), c.getMonto(), c.getMetodoPago()))
                .toList();
        entity.getCobros().addAll(cobros);
        return entity;
    }
}
