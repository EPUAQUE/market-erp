package com.ais.marketbackend.caja.infrastructure.persistence.adapters;

import com.ais.marketbackend.caja.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.caja.domain.model.CajaSesion;
import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
import com.ais.marketbackend.caja.domain.repository.CajaSesionRepository;
import com.ais.marketbackend.caja.infrastructure.persistence.mappers.CajaSesionEntityMapper;
import com.ais.marketbackend.caja.infrastructure.persistence.repositories.CajaSesionJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CajaSesionRepositoryAdapter implements CajaSesionRepository {

    private final CajaSesionJpaRepository jpaRepository;
    private final CajaSesionEntityMapper mapper;

    public CajaSesionRepositoryAdapter(CajaSesionJpaRepository jpaRepository, CajaSesionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CajaSesion save(CajaSesion sesion) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(sesion)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda indicada no existe.");
        }
    }

    @Override
    public Optional<CajaSesion> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CajaSesion> findAbiertaByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaIdAndEstado(tiendaId, EstadoCajaSesion.ABIERTA).map(mapper::toDomain);
    }

    @Override
    public Optional<CajaSesion> findAbiertaByTiendaIdConBloqueo(Long tiendaId) {
        return jpaRepository.findByTiendaIdAndEstadoConBloqueo(tiendaId, EstadoCajaSesion.ABIERTA)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CajaSesion> findByTiendaIdAndCorrelationIdApertura(Long tiendaId, String correlationIdApertura) {
        return jpaRepository.findByTiendaIdAndCorrelationIdApertura(tiendaId, correlationIdApertura)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CajaSesion> findByTiendaIdAndCorrelationIdCierre(Long tiendaId, String correlationIdCierre) {
        return jpaRepository.findByTiendaIdAndCorrelationIdCierre(tiendaId, correlationIdCierre)
                .map(mapper::toDomain);
    }

    @Override
    public List<CajaSesion> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<CajaSesion> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }
}
