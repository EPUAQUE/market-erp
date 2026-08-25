package com.ais.marketbackend.notificaciones.infrastructure.persistence.adapters;

import com.ais.marketbackend.notificaciones.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.domain.repository.NotificacionRepository;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.mappers.NotificacionEntityMapper;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.repositories.NotificacionJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class NotificacionRepositoryAdapter implements NotificacionRepository {

    private final NotificacionJpaRepository jpaRepository;
    private final NotificacionEntityMapper mapper;

    public NotificacionRepositoryAdapter(NotificacionJpaRepository jpaRepository, NotificacionEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Notificacion save(Notificacion notificacion) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(notificacion)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda indicada no existe.");
        }
    }

    @Override
    public Optional<Notificacion> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notificacion> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId) {
        return jpaRepository.findByTiendaIdAndLeidaFalse(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(
            Long tiendaId, TipoNotificacion tipo, Long referenciaId) {
        return jpaRepository.existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(tiendaId, tipo, referenciaId);
    }
}
