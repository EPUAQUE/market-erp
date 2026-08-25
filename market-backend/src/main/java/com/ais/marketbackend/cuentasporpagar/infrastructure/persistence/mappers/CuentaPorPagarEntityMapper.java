package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.mappers;

import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import com.ais.marketbackend.cuentasporpagar.domain.model.Pago;
import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities.CuentaPorPagarEntity;
import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities.PagoEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code pagos} necesita reconstruir la referencia
 * bidireccional {@code PagoEntity.cuenta}, igual que {@code CompraEntityMapper}.
 */
@Component
public class CuentaPorPagarEntityMapper {

    public CuentaPorPagar toDomain(CuentaPorPagarEntity entity) {
        List<Pago> pagos = entity.getPagos().stream()
                .map(p -> new Pago(p.getId(), p.getFecha(), p.getMonto()))
                .toList();
        return new CuentaPorPagar(
                entity.getId(), entity.getCompraId(), entity.getProveedorId(), entity.getTiendaId(),
                entity.getFechaEmision(), entity.getFechaVencimiento(), entity.getMontoOriginal(),
                entity.getSaldoPendiente(), entity.getEstado(), pagos);
    }

    public CuentaPorPagarEntity toEntity(CuentaPorPagar domain) {
        CuentaPorPagarEntity entity = new CuentaPorPagarEntity(
                domain.getId(), domain.getCompraId(), domain.getProveedorId(), domain.getTiendaId(),
                domain.getFechaEmision(), domain.getFechaVencimiento(), domain.getMontoOriginal(),
                domain.getSaldoPendiente(), domain.getEstado());
        List<PagoEntity> pagos = domain.getPagos().stream()
                .map(p -> new PagoEntity(p.getId(), entity, p.getFecha(), p.getMonto()))
                .toList();
        entity.getPagos().addAll(pagos);
        return entity;
    }
}
