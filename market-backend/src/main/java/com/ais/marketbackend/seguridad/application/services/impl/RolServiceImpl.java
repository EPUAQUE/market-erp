package com.ais.marketbackend.seguridad.application.services.impl;

import com.ais.marketbackend.seguridad.application.dtos.RolResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.seguridad.application.services.interfaces.RolService;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final AutorizacionTiendaService autorizacionTiendaService;

    public RolServiceImpl(RolRepository rolRepository, AutorizacionTiendaService autorizacionTiendaService) {
        this.rolRepository = rolRepository;
        this.autorizacionTiendaService = autorizacionTiendaService;
    }

    /**
     * Un solicitante de alcance no global (p. ej. ADMIN_GRUPO) no ve roles de
     * alcance global en este selector — evita que pueda asignarle a otro usuario
     * un rol con más alcance del que el propio solicitante tiene.
     */
    @Override
    public List<RolResumen> listar() {
        boolean alcanceGlobal = autorizacionTiendaService.tiendaIdsPermitidas().isEmpty();
        return rolRepository.findAll().stream()
                .filter(rol -> alcanceGlobal || !rol.isAlcanceGlobal())
                .map(this::toResumen)
                .toList();
    }

    private RolResumen toResumen(Rol rol) {
        return new RolResumen(rol.getId(), rol.getNombre(), rol.isAlcanceGlobal());
    }
}
