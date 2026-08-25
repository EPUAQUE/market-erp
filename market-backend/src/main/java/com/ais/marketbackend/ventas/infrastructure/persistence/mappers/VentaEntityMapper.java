package com.ais.marketbackend.ventas.infrastructure.persistence.mappers;

import com.ais.marketbackend.ventas.domain.model.LineaVenta;
import com.ais.marketbackend.ventas.domain.model.Venta;
import com.ais.marketbackend.ventas.infrastructure.persistence.entities.LineaVentaEntity;
import com.ais.marketbackend.ventas.infrastructure.persistence.entities.VentaEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code lineas} necesita reconstruir la referencia
 * bidireccional {@code LineaVentaEntity.venta}, igual que {@code CompraEntityMapper}.
 */
@Component
public class VentaEntityMapper {

    public Venta toDomain(VentaEntity entity) {
        List<LineaVenta> lineas = entity.getLineas().stream()
                .map(l -> new LineaVenta(l.getId(), l.getProductoId(), l.getCantidad(), l.getPrecioUnitario()))
                .toList();
        return new Venta(entity.getId(), entity.getClienteId(), entity.getTiendaId(), entity.getVendedorId(),
                entity.getFecha(), entity.getEstado(), lineas, entity.getMetodoPago(), entity.getCorrelationId());
    }

    public VentaEntity toEntity(Venta domain) {
        VentaEntity entity = new VentaEntity(
                domain.getId(), domain.getClienteId(), domain.getTiendaId(), domain.getVendedorId(),
                domain.getFecha(), domain.getEstado(), domain.getMetodoPago(), domain.getCorrelationId());
        List<LineaVentaEntity> lineas = domain.getLineas().stream()
                .map(l -> new LineaVentaEntity(l.getId(), entity, l.getProductoId(), l.getCantidad(), l.getPrecioUnitario()))
                .toList();
        entity.getLineas().addAll(lineas);
        return entity;
    }
}
