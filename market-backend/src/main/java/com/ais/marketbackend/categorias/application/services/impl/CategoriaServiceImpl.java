package com.ais.marketbackend.categorias.application.services.impl;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.categorias.domain.exception.CategoriaDuplicadaException;
import com.ais.marketbackend.categorias.domain.model.Categoria;
import com.ais.marketbackend.categorias.domain.repository.CategoriaRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional
    public CategoriaResumen crear(String nombre, String imagen) {
        if (categoriaRepository.existsByNombre(nombre)) {
            throw new CategoriaDuplicadaException(nombre);
        }
        return toResumen(categoriaRepository.save(Categoria.nueva(nombre, imagen)));
    }

    @Override
    @Transactional
    public CategoriaResumen actualizar(Long id, String nombre, String imagen) {
        Categoria categoria = obtenerORequerida(id);
        categoria.actualizarDatos(nombre, imagen);
        return toResumen(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Categoria categoria = obtenerORequerida(id);
        categoria.activar();
        categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Categoria categoria = obtenerORequerida(id);
        categoria.desactivar();
        categoriaRepository.save(categoria);
    }

    @Override
    public List<CategoriaResumen> listar() {
        return categoriaRepository.findAll().stream().map(this::toResumen).toList();
    }

    private Categoria obtenerORequerida(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    private CategoriaResumen toResumen(Categoria categoria) {
        return new CategoriaResumen(categoria.getId(), categoria.getNombre(), categoria.getImagen(), categoria.getEstado());
    }
}
