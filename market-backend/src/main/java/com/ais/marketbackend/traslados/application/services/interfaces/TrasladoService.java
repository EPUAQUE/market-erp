package com.ais.marketbackend.traslados.application.services.interfaces;

import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.traslados.application.dtos.NuevaLineaTraslado;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import java.util.List;

public interface TrasladoService {

    TrasladoResumen crear(Long tiendaOrigenId, Long tiendaDestinoId, List<NuevaLineaTraslado> lineas);

    /** Transiciona BORRADOR -> COMPLETADO y registra TRASLADO_SALIDA/TRASLADO_ENTRADA en Inventario por cada línea. */
    TrasladoResumen completar(Long id);

    TrasladoResumen anular(Long id);

    TrasladoResumen obtener(Long id);

    /** Sin paginar — uso interno. El endpoint público usa la variante paginada. */
    List<TrasladoResumen> listar();

    Pagina<TrasladoResumen> listar(int pagina, int tamano);
}
