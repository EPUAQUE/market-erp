package com.ais.marketbackend.compras.application.services.interfaces;

import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import java.util.List;

public interface CompraService {

    CompraResumen crear(Long tiendaId, Long proveedorId, List<NuevaLineaCompra> lineas);

    /** Transiciona BORRADOR -> RECIBIDA y registra un movimiento COMPRA en Inventario por cada línea. */
    CompraResumen recibir(Long tiendaId, Long id);

    CompraResumen anular(Long tiendaId, Long id);

    CompraResumen obtener(Long tiendaId, Long id);

    List<CompraResumen> listarPorTienda(Long tiendaId);
}
