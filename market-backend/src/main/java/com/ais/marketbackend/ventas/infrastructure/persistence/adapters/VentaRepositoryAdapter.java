package com.ais.marketbackend.ventas.infrastructure.persistence.adapters;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import com.ais.marketbackend.ventas.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.ventas.domain.model.Venta;
import com.ais.marketbackend.ventas.domain.repository.VentaRepository;
import com.ais.marketbackend.ventas.infrastructure.persistence.mappers.VentaEntityMapper;
import com.ais.marketbackend.ventas.infrastructure.persistence.repositories.VentaJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class VentaRepositoryAdapter implements VentaRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "fecha");

    private final VentaJpaRepository jpaRepository;
    private final VentaEntityMapper mapper;

    public VentaRepositoryAdapter(VentaJpaRepository jpaRepository, VentaEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Venta save(Venta venta) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(venta)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("El cliente, la tienda o el producto indicado no existe.");
        }
    }

    @Override
    public Optional<Venta> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Venta> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Venta> findByTiendaIdAndVendedorIdAndCorrelationId(
            Long tiendaId, Long vendedorId, String correlationId) {
        return jpaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(tiendaId, vendedorId, correlationId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Venta> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<Venta> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository
                .findByTiendaId(tiendaId, PageRequest.of(pagina, tamano, MAS_RECIENTE_PRIMERO))
                .map(mapper::toDomain));
    }
}
