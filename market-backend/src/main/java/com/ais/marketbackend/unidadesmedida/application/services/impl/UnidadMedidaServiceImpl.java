package com.ais.marketbackend.unidadesmedida.application.services.impl;

import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import com.ais.marketbackend.unidadesmedida.domain.exception.UnidadMedidaDuplicadaException;
import com.ais.marketbackend.unidadesmedida.domain.model.UnidadMedida;
import com.ais.marketbackend.unidadesmedida.domain.repository.UnidadMedidaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadMedidaServiceImpl implements UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;

    public UnidadMedidaServiceImpl(UnidadMedidaRepository unidadMedidaRepository) {
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @Override
    @Transactional
    public UnidadMedidaResumen crear(String nombre, String abreviacion) {
        if (unidadMedidaRepository.existsByNombre(nombre)) {
            throw new UnidadMedidaDuplicadaException(nombre);
        }
        UnidadMedida guardada = unidadMedidaRepository.save(UnidadMedida.nueva(nombre, abreviacion));
        return toResumen(guardada);
    }

    @Override
    @Transactional
    public UnidadMedidaResumen actualizar(Long id, String nombre, String abreviacion) {
        UnidadMedida unidadMedida = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada: " + id));
        unidadMedida.actualizar(nombre, abreviacion);
        return toResumen(unidadMedidaRepository.save(unidadMedida));
    }

    @Override
    public List<UnidadMedidaResumen> listar() {
        return unidadMedidaRepository.findAll().stream().map(this::toResumen).toList();
    }

    private UnidadMedidaResumen toResumen(UnidadMedida unidadMedida) {
        return new UnidadMedidaResumen(unidadMedida.getId(), unidadMedida.getNombre(), unidadMedida.getAbreviacion());
    }
}
