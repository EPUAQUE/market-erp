package com.ais.marketbackend.fel.infrastructure.persistence.adapters;

import com.ais.marketbackend.fel.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import com.ais.marketbackend.fel.domain.repository.DocumentoFelRepository;
import com.ais.marketbackend.fel.infrastructure.persistence.entities.FelCorrelativoEntity;
import com.ais.marketbackend.fel.infrastructure.persistence.mappers.DocumentoFelEntityMapper;
import com.ais.marketbackend.fel.infrastructure.persistence.repositories.DocumentoFelJpaRepository;
import com.ais.marketbackend.fel.infrastructure.persistence.repositories.FelCorrelativoJpaRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.infrastructure.persistence.PaginaMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DocumentoFelRepositoryAdapter implements DocumentoFelRepository {

    private final DocumentoFelJpaRepository jpaRepository;
    private final FelCorrelativoJpaRepository correlativoJpaRepository;
    private final DocumentoFelEntityMapper mapper;
    private final TransactionTemplate nuevaTransaccionCorrelativo;

    public DocumentoFelRepositoryAdapter(
            DocumentoFelJpaRepository jpaRepository, FelCorrelativoJpaRepository correlativoJpaRepository,
            DocumentoFelEntityMapper mapper, PlatformTransactionManager transactionManager) {
        this.jpaRepository = jpaRepository;
        this.correlativoJpaRepository = correlativoJpaRepository;
        this.mapper = mapper;
        this.nuevaTransaccionCorrelativo = new TransactionTemplate(transactionManager);
        this.nuevaTransaccionCorrelativo.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public DocumentoFel save(DocumentoFel documento) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toEntity(documento)));
        } catch (DataIntegrityViolationException e) {
            throw new ReferenciaInvalidaException("La venta o la tienda indicada no existe.");
        }
    }

    @Override
    public Optional<DocumentoFel> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DocumentoFel> findByIdConBloqueo(Long id) {
        return jpaRepository.findByIdConBloqueo(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DocumentoFel> findByVentaId(Long ventaId) {
        return jpaRepository.findByVentaId(ventaId).map(mapper::toDomain);
    }

    @Override
    public List<DocumentoFel> findByTiendaId(Long tiendaId) {
        return jpaRepository.findByTiendaId(tiendaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Pagina<DocumentoFel> findByTiendaId(Long tiendaId, int pagina, int tamano) {
        return PaginaMapper.desde(
                jpaRepository.findByTiendaId(tiendaId, PageRequest.of(pagina, tamano)).map(mapper::toDomain));
    }

    /**
     * Reserva el número en una transacción propia ({@code REQUIRES_NEW}), separada de
     * la transacción de {@code emitir()}: así el número queda comprometido de inmediato
     * y nunca se reutiliza, aunque el resto de la emisión falle después. Bloquea la fila
     * del correlativo con {@code PESSIMISTIC_WRITE}; si dos emisiones concurrentes son
     * las primeras para (tienda, serie), la creación de la fila puede colisionar por la
     * restricción única — se reintenta una sola vez en una transacción nueva, igual que
     * el patrón de colisión de creación concurrente en {@code InventarioServiceImpl}.
     */
    @Override
    public long siguienteNumero(Long tiendaId, String serie) {
        try {
            return nuevaTransaccionCorrelativo.execute(status -> incrementarCorrelativo(tiendaId, serie));
        } catch (DataIntegrityViolationException colisionDeCreacionConcurrente) {
            return nuevaTransaccionCorrelativo.execute(status -> incrementarCorrelativo(tiendaId, serie));
        }
    }

    private long incrementarCorrelativo(Long tiendaId, String serie) {
        FelCorrelativoEntity correlativo = correlativoJpaRepository
                .findByTiendaIdAndSerieConBloqueo(tiendaId, serie)
                .orElseGet(() -> nuevoCorrelativo(tiendaId, serie));
        long numero = correlativo.getSiguienteNumero();
        correlativo.setSiguienteNumero(numero + 1);
        correlativoJpaRepository.save(correlativo);
        return numero;
    }

    private FelCorrelativoEntity nuevoCorrelativo(Long tiendaId, String serie) {
        long numeroInicial = jpaRepository.findMaxNumero(tiendaId, serie) + 1;
        return new FelCorrelativoEntity(null, tiendaId, serie, numeroInicial);
    }
}
