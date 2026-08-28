package com.ais.marketbackend.grupostienda.application.services.impl;

import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.grupostienda.domain.exception.GrupoTiendaDuplicadoException;
import com.ais.marketbackend.grupostienda.domain.model.GrupoTienda;
import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrupoTiendaServiceImpl implements GrupoTiendaService {

    private final GrupoTiendaRepository grupoTiendaRepository;
    private final AutorizacionTiendaService autorizacionTiendaService;

    public GrupoTiendaServiceImpl(
            GrupoTiendaRepository grupoTiendaRepository, AutorizacionTiendaService autorizacionTiendaService) {
        this.grupoTiendaRepository = grupoTiendaRepository;
        this.autorizacionTiendaService = autorizacionTiendaService;
    }

    @Override
    @Transactional
    public GrupoTiendaResumen crear(String codigo, String nombre) {
        String codigoCanonico = canonicalizarCodigo(codigo);
        if (grupoTiendaRepository.existsByCodigo(codigoCanonico)) {
            throw new GrupoTiendaDuplicadoException(codigoCanonico);
        }
        GrupoTienda grupoTienda = GrupoTienda.nuevo(codigoCanonico, nombre);
        return toResumen(grupoTiendaRepository.save(grupoTienda));
    }

    @Override
    @Transactional
    public GrupoTiendaResumen actualizar(Long id, String nombre) {
        autorizacionTiendaService.exigirAccesoAGrupo(id);
        GrupoTienda grupoTienda = obtenerORequerido(id);
        grupoTienda.actualizarDatos(nombre);
        return toResumen(grupoTiendaRepository.save(grupoTienda));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        autorizacionTiendaService.exigirAccesoAGrupo(id);
        GrupoTienda grupoTienda = obtenerORequerido(id);
        grupoTienda.activar();
        grupoTiendaRepository.save(grupoTienda);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        autorizacionTiendaService.exigirAccesoAGrupo(id);
        GrupoTienda grupoTienda = obtenerORequerido(id);
        grupoTienda.desactivar();
        grupoTiendaRepository.save(grupoTienda);
    }

    @Override
    public List<GrupoTiendaResumen> listar() {
        Optional<Set<Long>> grupoIdsPermitidas = autorizacionTiendaService.grupoIdsPermitidas();
        return grupoTiendaRepository.findAll().stream()
                .filter(grupo -> grupoIdsPermitidas.isEmpty() || grupoIdsPermitidas.get().contains(grupo.getId()))
                .map(this::toResumen)
                .toList();
    }

    private GrupoTienda obtenerORequerido(Long id) {
        return grupoTiendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de tiendas no encontrado: " + id));
    }

    private String canonicalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private GrupoTiendaResumen toResumen(GrupoTienda grupoTienda) {
        return new GrupoTiendaResumen(
                grupoTienda.getId(), grupoTienda.getCodigo(), grupoTienda.getNombre(), grupoTienda.getEstado());
    }
}
