package com.ais.marketbackend.compras.infrastructure.persistence.mappers;

import com.ais.marketbackend.compras.domain.model.Compra;
import com.ais.marketbackend.compras.domain.model.LineaCompra;
import com.ais.marketbackend.compras.infrastructure.persistence.entities.CompraEntity;
import com.ais.marketbackend.compras.infrastructure.persistence.entities.LineaCompraEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code lineas} necesita reconstruir la referencia
 * bidireccional {@code LineaCompraEntity.compra}, algo que MapStruct no resuelve
 * bien para un {@code @OneToMany} con {@code orphanRemoval}.
 */
@Component
public class CompraEntityMapper {

    public Compra toDomain(CompraEntity entity) {
        List<LineaCompra> lineas = entity.getLineas().stream()
                .map(l -> new LineaCompra(l.getId(), l.getProductoId(), l.getCantidad(), l.getCostoUnitario()))
                .toList();
        return new Compra(entity.getId(), entity.getProveedorId(), entity.getTiendaId(), entity.getFecha(),
                entity.getEstado(), lineas);
    }

    public CompraEntity toEntity(Compra domain) {
        CompraEntity entity = new CompraEntity(
                domain.getId(), domain.getProveedorId(), domain.getTiendaId(), domain.getFecha(), domain.getEstado());
        List<LineaCompraEntity> lineas = domain.getLineas().stream()
                .map(l -> new LineaCompraEntity(l.getId(), entity, l.getProductoId(), l.getCantidad(), l.getCostoUnitario()))
                .toList();
        entity.getLineas().addAll(lineas);
        return entity;
    }
}
