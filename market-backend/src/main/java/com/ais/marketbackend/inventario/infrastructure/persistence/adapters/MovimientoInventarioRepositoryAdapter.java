package com.ais.marketbackend.inventario.infrastructure.persistence.adapters;

import com.ais.marketbackend.inventario.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.inventario.domain.model.MovimientoInventario;
import com.ais.marketbackend.inventario.domain.repository.MovimientoInventarioRepository;
import com.ais.marketbackend.inventario.infrastructure.persistence.mappers.MovimientoInventarioEntityMapper;
import com.ais.marketbackend.inventario.infrastructure.persistence.repositories.MovimientoInventarioJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class MovimientoInventarioRepositoryAdapter implements MovimientoInventarioRepository {

    private final MovimientoInventarioJpaRepository jpaRepository;
    private final MovimientoInventarioEntityMapper mapper;

    public MovimientoInventarioRepositoryAdapter(
            MovimientoInventarioJpaRepository jpaRepository, MovimientoInventarioEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MovimientoInventario registrar(MovimientoInventario movimiento) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(movimiento)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda o el producto indicado no existe.");
        }
    }

    @Override
    public List<MovimientoInventario> findByTiendaIdAndProductoIdOrderByFechaDesc(Long tiendaId, Long productoId) {
        return jpaRepository.findByTiendaIdAndProductoIdOrderByFechaDesc(tiendaId, productoId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Pagina<MovimientoInventario> findByTiendaIdAndProductoIdOrderByFechaDesc(
            Long tiendaId, Long productoId, int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository
                .findByTiendaIdAndProductoIdOrderByFechaDesc(tiendaId, productoId, PageRequest.of(pagina, tamano))
                .map(mapper::toDomain));
    }
}
