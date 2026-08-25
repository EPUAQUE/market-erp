package com.ais.marketbackend.productos.api.controllers;

import com.ais.marketbackend.productos.api.dtos.responses.ProductoTiendaResponse;
import com.ais.marketbackend.productos.api.mappers.ProductoTiendaApiMapper;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import com.ais.marketbackend.shared.api.PaginacionParams;
import com.ais.marketbackend.shared.responses.PaginaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de venta de una tienda: precio/stock mínimo-máximo/permitirVenta por
 * producto, ya filtrable por tienda vía {@code PermissionInterceptor} (a
 * diferencia de {@code ProductoTiendaController}, que solo lista por producto).
 * Pensado para el POS — combina con {@code GET /productos} (nombre/código/imagen,
 * catálogo global) y {@code GET /inventario/tiendas/{tiendaId}} (existencia) del
 * lado del cliente.
 */
@RestController
@RequestMapping("/api/v1/productos/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class ProductoCatalogoController {

    private final ProductoTiendaService productoTiendaService;
    private final ProductoTiendaApiMapper mapper;

    @GetMapping
    @RequiresPermission("PRODUCTOS_VER")
    public ResponseEntity<PaginaResponse<ProductoTiendaResponse>> listar(
            @PathVariable Long tiendaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = productoTiendaService.listarPorTienda(
                tiendaId, PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }
}
