package com.ais.marketbackend.productos.infrastructure.persistence.adapters;

import com.ais.marketbackend.productos.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.productos.domain.model.ProductoTienda;
import com.ais.marketbackend.productos.domain.repository.ProductoTiendaRepository;
import com.ais.marketbackend.productos.infrastructure.persistence.mappers.ProductoTiendaEntityMapper;
import com.ais.marketbackend.productos.infrastructure.persistence.repositories.ProductoTiendaJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ProductoTiendaRepositoryAdapter implements ProductoTiendaRepository {

    private final ProductoTiendaJpaRepository jpaRepository;
    private final ProductoTiendaEntityMapper mapper;

    public ProductoTiendaRepositoryAdapter(ProductoTiendaJpaRepository jpaRepository, ProductoTiendaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductoTienda save(ProductoTienda productoTienda) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(productoTienda)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("El producto o la tienda indicada no existe.");
        }
    }

    @Override
    public Optional<ProductoTienda> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ProductoTienda> findByProductoIdAndTiendaId(Long productoId, Long tiendaId) {
        return jpaRepository.findByProductoIdAndTiendaId(productoId, tiendaId).map(mapper::toDomain);
    }

    @Override
    public List<ProductoTienda> findByProductoId(Long productoId) {
        return jpaRepository.findByProductoId(productoId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ProductoTienda> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<ProductoTienda> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
