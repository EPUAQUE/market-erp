package com.ais.marketbackend.tiendas.application.services.interfaces;

import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import java.util.List;

public interface TiendaService {

    TiendaResumen crear(String codigo, String nombre, String direccion, String telefono, String correo, Long grupoId);

    TiendaResumen actualizar(Long id, String nombre, String direccion, String telefono, String correo, Long grupoId);

    void activar(Long id);

    void desactivar(Long id);

    List<TiendaResumen> listar();

    /**
     * Sin filtro por {@code tiendaIdsPermitidas()} del usuario actual (a diferencia
     * de {@link #listar()}) — uso interno entre módulos, ver {@code InventarioServiceImpl}
     * (consulta de existencias del grupo de la tienda propia del usuario, que sí incluye
     * tiendas hermanas fuera de su alcance individual).
     */
    TiendaResumen obtener(Long id);

    /** Mismo criterio sin filtro de alcance que {@link #obtener(Long)} — ver ahí. */
    List<TiendaResumen> listarPorGrupo(Long grupoId);
}
