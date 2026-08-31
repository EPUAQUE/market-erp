package com.ais.marketbackend.notificaciones.infrastructure.persistence.adapters;

import com.ais.marketbackend.notificaciones.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.notificaciones.domain.model.Notificacion;
import com.ais.marketbackend.notificaciones.domain.model.TipoNotificacion;
import com.ais.marketbackend.notificaciones.domain.repository.NotificacionRepository;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.mappers.NotificacionEntityMapper;
import com.ais.marketbackend.notificaciones.infrastructure.persistence.repositories.NotificacionJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
    public Pagina<Notificacion> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }

    @Override
    public List<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId) {
        return jpaRepository.findByTiendaIdAndLeidaFalse(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<Notificacion> findByTiendaIdAndLeidaFalse(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaIdAndLeidaFalse(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }

    @Override
    public boolean existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(
            Long tiendaId, TipoNotificacion tipo, Long referenciaId) {
        return jpaRepository.existsByTiendaIdAndTipoAndReferenciaIdAndLeidaFalse(tiendaId, tipo, referenciaId);
    }
}
