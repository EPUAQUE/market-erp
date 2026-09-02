package com.ais.marketbackend.productos.infrastructure.persistence.adapters;

import com.ais.marketbackend.productos.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.domain.repository.ProductoRepository;
import com.ais.marketbackend.productos.infrastructure.persistence.mappers.ProductoEntityMapper;
import com.ais.marketbackend.productos.infrastructure.persistence.repositories.ProductoJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductoRepositoryAdapter implements ProductoRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "id");

    private final ProductoJpaRepository jpaRepository;
    private final ProductoEntityMapper mapper;

    public ProductoRepositoryAdapter(ProductoJpaRepository jpaRepository, ProductoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Producto save(Producto producto) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(producto)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La categoría, marca o unidad de medida indicada no existe.");
        }
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCodigoInterno(String codigoInterno) {
        return jpaRepository.existsByCodigoInterno(codigoInterno);
    }

    @Override
    public List<Producto> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<Producto> findAll(int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findAll(PageRequest.of(pagina, tamano, MAS_RECIENTE_PRIMERO)).map(mapper::toDomain));
    }
}
