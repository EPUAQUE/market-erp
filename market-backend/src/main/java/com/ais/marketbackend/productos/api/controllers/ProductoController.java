package com.ais.marketbackend.productos.api.controllers;

import com.ais.marketbackend.productos.api.dtos.requests.ActualizarProductoRequest;
import com.ais.marketbackend.productos.api.dtos.requests.CrearProductoRequest;
import com.ais.marketbackend.productos.api.dtos.responses.ProductoResponse;
import com.ais.marketbackend.productos.api.mappers.ProductoApiMapper;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.infrastructure.storage.ImagenProductoAlmacenamientoService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final ProductoApiMapper mapper;
    private final ImagenProductoAlmacenamientoService imagenAlmacenamientoService;

    @GetMapping
    @RequiresPermission("PRODUCTOS_VER")
    public ResponseEntity<PaginaResponse<ProductoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = productoService.listar(
                PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @PostMapping
    @RequiresPermission("PRODUCTOS_CREAR")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        ProductoResponse creado = mapper.toResponse(productoService.crear(
                request.codigoInterno(), request.codigoBarras(), request.nombre(), request.descripcion(),
                request.categoriaId(), request.marcaId(), request.unidadMedidaId(), request.imagenUrl()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        ProductoResponse actualizado = mapper.toResponse(productoService.actualizar(
                id, request.codigoBarras(), request.nombre(), request.descripcion(), request.categoriaId(),
                request.marcaId(), request.unidadMedidaId(), request.imagenUrl()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping(value = "/{id}/imagen", consumes = "multipart/form-data")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<ProductoResponse> subirImagen(
            @PathVariable Long id, @RequestParam("archivo") MultipartFile archivo) {
        String imagenUrl = imagenAlmacenamientoService.guardar(archivo);
        ProductoResponse actualizado = mapper.toResponse(productoService.actualizarImagen(id, imagenUrl));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        productoService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("PRODUCTOS_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
