package com.ais.marketbackend.tiendas.application.services.impl;

import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.tiendas.domain.exception.TiendaDuplicadaException;
import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TiendaServiceImpl implements TiendaService {

    private final TiendaRepository tiendaRepository;
    private final GrupoTiendaRepository grupoTiendaRepository;

    public TiendaServiceImpl(TiendaRepository tiendaRepository, GrupoTiendaRepository grupoTiendaRepository) {
        this.tiendaRepository = tiendaRepository;
        this.grupoTiendaRepository = grupoTiendaRepository;
    }

    @Override
    @Transactional
    public TiendaResumen crear(
            String codigo, String nombre, String direccion, String telefono, String correo, Long grupoId) {
        String codigoCanonico = canonicalizarCodigo(codigo);
        if (tiendaRepository.existsByCodigo(codigoCanonico)) {
            throw new TiendaDuplicadaException(codigoCanonico);
        }
        exigirGrupoExistente(grupoId);
        Tienda tienda = Tienda.nueva(codigoCanonico, nombre, direccion, telefono, correo, grupoId);
        return toResumen(tiendaRepository.save(tienda));
    }

    @Override
    @Transactional
    public TiendaResumen actualizar(
            Long id, String nombre, String direccion, String telefono, String correo, Long grupoId) {
        Tienda tienda = obtenerORequerido(id);
        exigirGrupoExistente(grupoId);
        tienda.actualizarDatos(nombre, direccion, telefono, correo);
        tienda.reasignarGrupo(grupoId);
        return toResumen(tiendaRepository.save(tienda));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Tienda tienda = obtenerORequerido(id);
        tienda.activar();
        tiendaRepository.save(tienda);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Tienda tienda = obtenerORequerido(id);
        tienda.desactivar();
        tiendaRepository.save(tienda);
    }

    @Override
    public List<TiendaResumen> listar() {
        return tiendaRepository.findAll().stream().map(this::toResumen).toList();
    }

    private Tienda obtenerORequerido(Long id) {
        return tiendaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + id));
    }

    private void exigirGrupoExistente(Long grupoId) {
        if (grupoTiendaRepository.findById(grupoId).isEmpty()) {
            throw new ResourceNotFoundException("Grupo de tiendas no encontrado: " + grupoId);
        }
    }

    private String canonicalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    private TiendaResumen toResumen(Tienda tienda) {
        return new TiendaResumen(
                tienda.getId(), tienda.getCodigo(), tienda.getNombre(), tienda.getDireccion(),
                tienda.getTelefono(), tienda.getCorreo(), tienda.getEstado(), tienda.getGrupoId());
    }
}
