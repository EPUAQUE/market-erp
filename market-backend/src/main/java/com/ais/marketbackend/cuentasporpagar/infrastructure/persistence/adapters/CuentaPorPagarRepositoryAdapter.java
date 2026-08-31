package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.adapters;

import com.ais.marketbackend.cuentasporpagar.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.cuentasporpagar.domain.model.CuentaPorPagar;
import com.ais.marketbackend.cuentasporpagar.domain.repository.CuentaPorPagarRepository;
import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.mappers.CuentaPorPagarEntityMapper;
import com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.repositories.CuentaPorPagarJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CuentaPorPagarRepositoryAdapter implements CuentaPorPagarRepository {

    private final CuentaPorPagarJpaRepository jpaRepository;
    private final CuentaPorPagarEntityMapper mapper;

    public CuentaPorPagarRepositoryAdapter(
            CuentaPorPagarJpaRepository jpaRepository, CuentaPorPagarEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CuentaPorPagar save(CuentaPorPagar cuenta) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(cuenta)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La compra, el proveedor o la tienda indicada no existe.");
        }
    }

    @Override
    public Optional<CuentaPorPagar> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CuentaPorPagar> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public List<CuentaPorPagar> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<CuentaPorPagar> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
