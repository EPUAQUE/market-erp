package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.adapters;

import com.ais.marketbackend.cuentasporcobrar.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.cuentasporcobrar.domain.model.CuentaPorCobrar;
import com.ais.marketbackend.cuentasporcobrar.domain.repository.CuentaPorCobrarRepository;
import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.mappers.CuentaPorCobrarEntityMapper;
import com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.repositories.CuentaPorCobrarJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CuentaPorCobrarRepositoryAdapter implements CuentaPorCobrarRepository {

    private final CuentaPorCobrarJpaRepository jpaRepository;
    private final CuentaPorCobrarEntityMapper mapper;

    public CuentaPorCobrarRepositoryAdapter(
            CuentaPorCobrarJpaRepository jpaRepository, CuentaPorCobrarEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CuentaPorCobrar save(CuentaPorCobrar cuenta) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(cuenta)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La venta, el cliente o la tienda indicada no existe.");
        }
    }

    @Override
    public Optional<CuentaPorCobrar> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CuentaPorCobrar> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public List<CuentaPorCobrar> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<CuentaPorCobrar> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }

    @Override
    public Optional<CuentaPorCobrar> findByVentaId(Long ventaId) {
        return jpaRepository.findByVentaId(ventaId).map(mapper::toDomain);
    }
}
