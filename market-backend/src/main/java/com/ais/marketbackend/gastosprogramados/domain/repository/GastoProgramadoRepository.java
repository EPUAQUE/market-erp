package com.ais.marketbackend.gastosprogramados.domain.repository;

import com.ais.marketbackend.gastosprogramados.domain.model.GastoProgramado;
import java.util.List;
import java.util.Optional;

public interface GastoProgramadoRepository {

    GastoProgramado save(GastoProgramado gasto);

    Optional<GastoProgramado> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code GastoProgramadoServiceImpl.generarPago} para serializar
     * ejecuciones concurrentes del mismo gasto. Sin esto, dos ejecuciones casi
     * simultáneas podían leer la misma {@code proximaFecha} vencida y ambas
     * pasar la validación, generando dos pagos para el mismo período (y
     * arriesgando perder uno de los dos por la colección JPA con
     * {@code orphanRemoval}, igual que Caja/CxC/CxP).
     */
    Optional<GastoProgramado> findByIdConBloqueo(Long id);

    List<GastoProgramado> findByTiendaId(Long tiendaId);
}
