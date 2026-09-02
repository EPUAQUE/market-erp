package com.ais.marketbackend.productos.application.services.impl;

import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.domain.exception.ProductoDuplicadoException;
import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.domain.repository.ProductoRepository;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public ProductoResumen crear(
            String codigoInterno, String codigoBarras, String nombre, String descripcion, String descripcionCorta,
            Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl) {
        if (productoRepository.existsByCodigoInterno(codigoInterno)) {
            throw new ProductoDuplicadoException(codigoInterno);
        }
        Producto producto = Producto.nuevo(
                codigoInterno, codigoBarras, nombre, descripcion, descripcionCorta, categoriaId, marcaId,
                unidadMedidaId, imagenUrl);
        return toResumen(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResumen actualizar(
            Long id, String codigoBarras, String nombre, String descripcion, String descripcionCorta,
            Long categoriaId, Long marcaId, Long unidadMedidaId, String imagenUrl) {
        Producto producto = obtenerORequerido(id);
        producto.actualizarDatos(
                codigoBarras, nombre, descripcion, descripcionCorta, categoriaId, marcaId, unidadMedidaId, imagenUrl);
        return toResumen(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResumen actualizarImagen(Long id, String imagenUrl) {
        Producto producto = obtenerORequerido(id);
        producto.actualizarImagen(imagenUrl);
        return toResumen(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void activar(Long id) {
        Producto producto = obtenerORequerido(id);
        producto.activar();
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Producto producto = obtenerORequerido(id);
        producto.desactivar();
        productoRepository.save(producto);
    }

    @Override
    public List<ProductoResumen> listar() {
        return productoRepository.findAll().stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<ProductoResumen> listar(int pagina, int tamano) {
        return productoRepository.findAll(pagina, tamano).map(this::toResumen);
    }

    private Producto obtenerORequerido(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private ProductoResumen toResumen(Producto producto) {
        return new ProductoResumen(
                producto.getId(), producto.getCodigoInterno(), producto.getCodigoBarras(), producto.getNombre(),
                producto.getDescripcion(), producto.getDescripcionCorta(), producto.getCategoriaId(),
                producto.getMarcaId(), producto.getUnidadMedidaId(), producto.getImagenUrl(), producto.isActivo());
    }
}
