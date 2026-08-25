package com.ais.marketbackend.traslados.infrastructure.persistence.mappers;

import com.ais.marketbackend.traslados.domain.model.LineaTraslado;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import com.ais.marketbackend.traslados.infrastructure.persistence.entities.LineaTrasladoEntity;
import com.ais.marketbackend.traslados.infrastructure.persistence.entities.TrasladoEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapeo manual (no MapStruct): {@code lineas} necesita reconstruir la referencia
 * bidireccional {@code LineaTrasladoEntity.traslado}, igual que {@code CompraEntityMapper}.
 */
@Component
public class TrasladoEntityMapper {

    public Traslado toDomain(TrasladoEntity entity) {
        List<LineaTraslado> lineas = entity.getLineas().stream()
                .map(l -> new LineaTraslado(l.getId(), l.getProductoId(), l.getCantidad()))
                .toList();
        return new Traslado(
                entity.getId(), entity.getTiendaOrigenId(), entity.getTiendaDestinoId(), entity.getFecha(),
                entity.getEstado(), lineas);
    }

    public TrasladoEntity toEntity(Traslado domain) {
        TrasladoEntity entity = new TrasladoEntity(
                domain.getId(), domain.getTiendaOrigenId(), domain.getTiendaDestinoId(), domain.getFecha(),
                domain.getEstado());
        List<LineaTrasladoEntity> lineas = domain.getLineas().stream()
                .map(l -> new LineaTrasladoEntity(l.getId(), entity, l.getProductoId(), l.getCantidad()))
                .toList();
        entity.getLineas().addAll(lineas);
        return entity;
    }
}
