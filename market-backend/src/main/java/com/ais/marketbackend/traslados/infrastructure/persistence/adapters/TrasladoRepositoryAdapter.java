package com.ais.marketbackend.traslados.infrastructure.persistence.adapters;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import com.ais.marketbackend.traslados.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import com.ais.marketbackend.traslados.domain.repository.TrasladoRepository;
import com.ais.marketbackend.traslados.infrastructure.persistence.mappers.TrasladoEntityMapper;
import com.ais.marketbackend.traslados.infrastructure.persistence.repositories.TrasladoJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class TrasladoRepositoryAdapter implements TrasladoRepository {

    private final TrasladoJpaRepository jpaRepository;
    private final TrasladoEntityMapper mapper;

    public TrasladoRepositoryAdapter(TrasladoJpaRepository jpaRepository, TrasladoEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Traslado save(Traslado traslado) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(traslado)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La tienda o el producto indicado no existe.");
        }
    }

    @Override
    public Optional<Traslado> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Traslado> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public List<Traslado> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<Traslado> listar(int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository.findAll(PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }

    @Override
    public Pagina<Traslado> listarPorTiendas(Set<Long> tiendaIds, int pagina, int tamano) {
        return PaginaMapper.desde(jpaRepository
                .findByTiendaOrigenIdInOrTiendaDestinoIdIn(tiendaIds, tiendaIds, PageRequest.of(pagina, tamano))
                .map(mapper::toDomain));
    }
}
