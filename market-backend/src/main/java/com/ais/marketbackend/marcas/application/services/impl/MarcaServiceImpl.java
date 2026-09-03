package com.ais.marketbackend.marcas.application.services.impl;

import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.marcas.domain.exception.MarcaDuplicadaException;
import com.ais.marketbackend.marcas.domain.model.Marca;
import com.ais.marketbackend.marcas.domain.repository.MarcaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaServiceImpl(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    @Override
    @Transactional
    public MarcaResumen crear(String nombre) {
        if (marcaRepository.existsByNombre(nombre)) {
            throw new MarcaDuplicadaException(nombre);
        }
        return toResumen(marcaRepository.save(Marca.nueva(nombre)));
    }

    @Override
    @Transactional
    public MarcaResumen actualizar(Long id, String nombre) {
        Marca marca = obtenerORequerida(id);
        marca.actualizar(nombre);
        return toResumen(marcaRepository.save(marca));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Marca marca = obtenerORequerida(id);
        marca.activar();
        marcaRepository.save(marca);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Marca marca = obtenerORequerida(id);
        marca.desactivar();
        marcaRepository.save(marca);
    }

    @Override
    public List<MarcaResumen> listar() {
        return marcaRepository.findAll().stream().map(this::toResumen).toList();
    }

    private Marca obtenerORequerida(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada: " + id));
    }

    private MarcaResumen toResumen(Marca marca) {
        return new MarcaResumen(marca.getId(), marca.getNombre(), marca.getEstado());
    }
}
