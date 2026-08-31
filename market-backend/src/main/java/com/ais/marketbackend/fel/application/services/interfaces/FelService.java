package com.ais.marketbackend.fel.application.services.interfaces;

import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;

public interface FelService {

    /** Emite el documento FEL de una venta completada — falla si ya tiene uno o la venta no está completada. */
    DocumentoFelResumen emitir(Long tiendaId, Long ventaId);

    DocumentoFelResumen anular(Long tiendaId, Long id, String motivo);

    /** Reintenta la certificación de un documento en estado ERROR. */
    DocumentoFelResumen reintentar(Long tiendaId, Long id);

    DocumentoFelResumen obtener(Long tiendaId, Long id);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<DocumentoFelResumen> listarPorTienda(Long tiendaId);

    Pagina<DocumentoFelResumen> listarPorTienda(Long tiendaId, int pagina, int tamano);
}
