package com.ais.marketbackend.fel.domain.repository;

import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import java.util.List;
import java.util.Optional;

public interface DocumentoFelRepository {

    DocumentoFel save(DocumentoFel documento);

    Optional<DocumentoFel> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code FelServiceImpl.reintentar}/{@code anular} para serializar
     * transiciones de estado concurrentes sobre el mismo documento. Sin esto,
     * dos {@code reintentar} casi simultáneos sobre un documento en ERROR
     * podían ambos pasar la validación y llamar dos veces al certificador
     * externo, quedándose en BD solo con el resultado del que guarde último.
     */
    Optional<DocumentoFel> findByIdConBloqueo(Long id);

    Optional<DocumentoFel> findByVentaId(Long ventaId);

    List<DocumentoFel> findByTiendaId(Long tiendaId);

    /** Siguiente correlativo de la serie para la tienda — usado al emitir un nuevo documento. */
    long siguienteNumero(Long tiendaId, String serie);
}
