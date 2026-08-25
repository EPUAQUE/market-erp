package com.ais.marketbackend.inventario.infrastructure.persistence.adapters;

import com.ais.marketbackend.inventario.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.inventario.domain.model.Inventario;
import com.ais.marketbackend.inventario.domain.repository.InventarioRepository;
import com.ais.marketbackend.inventario.infrastructure.persistence.mappers.InventarioEntityMapper;
import com.ais.marketbackend.inventario.infrastructure.persistence.repositories.InventarioJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class InventarioRepositoryAdapter implements InventarioRepository {

    private final InventarioJpaRepository jpaRepository;
    private final InventarioEntityMapper mapper;

    public InventarioRepositoryAdapter(InventarioJpaRepository jpaRepository, InventarioEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Inventario save(Inventario inventario) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(inventario)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda o el producto indicado no existe.");
        }
    }

    @Override
    public Optional<Inventario> findByTiendaIdAndProductoId(Long tiendaId, Long productoId) {
        return jpaRepository.findByTiendaIdAndProductoId(tiendaId, productoId).map(mapper::toDomain);
    }

    @Override
    public Optional<Inventario> findByTiendaIdAndProductoIdConBloqueo(Long tiendaId, Long productoId) {
        return jpaRepository.findByTiendaIdAndProductoIdConBloqueo(tiendaId, productoId).map(mapper::toDomain);
    }

    @Override
    public List<Inventario> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<Inventario> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
