package com.ais.marketbackend.seguridad.application.services.impl;

import com.ais.marketbackend.seguridad.application.dtos.RolResumen;
import com.ais.marketbackend.seguridad.application.services.interfaces.RolService;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;

    public RolServiceImpl(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public List<RolResumen> listar() {
        return rolRepository.findAll().stream().map(this::toResumen).toList();
    }

    private RolResumen toResumen(Rol rol) {
        return new RolResumen(rol.getId(), rol.getNombre(), rol.isAlcanceGlobal());
    }
}
