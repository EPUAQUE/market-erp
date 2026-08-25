package com.ais.marketbackend.gastosprogramados.api.controllers;

import com.ais.marketbackend.gastosprogramados.api.dtos.requests.ActualizarGastoProgramadoRequest;
import com.ais.marketbackend.gastosprogramados.api.dtos.requests.CrearGastoProgramadoRequest;
import com.ais.marketbackend.gastosprogramados.api.dtos.responses.GastoProgramadoResponse;
import com.ais.marketbackend.gastosprogramados.api.mappers.GastoProgramadoApiMapper;
import com.ais.marketbackend.gastosprogramados.application.services.interfaces.GastoProgramadoService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gastos-programados/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class GastoProgramadoController {

    private final GastoProgramadoService gastoProgramadoService;
    private final GastoProgramadoApiMapper mapper;

    @GetMapping
    @RequiresPermission("GASTOS_PROGRAMADOS_VER")
    public ResponseEntity<List<GastoProgramadoResponse>> listar(@PathVariable Long tiendaId) {
        List<GastoProgramadoResponse> items =
                gastoProgramadoService.listarPorTienda(tiendaId).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    @RequiresPermission("GASTOS_PROGRAMADOS_VER")
    public ResponseEntity<GastoProgramadoResponse> obtener(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(gastoProgramadoService.obtener(tiendaId, id)));
    }

    @PostMapping
    @RequiresPermission("GASTOS_PROGRAMADOS_CREAR")
    public ResponseEntity<GastoProgramadoResponse> crear(
            @PathVariable Long tiendaId, @Valid @RequestBody CrearGastoProgramadoRequest request) {
        GastoProgramadoResponse creado = mapper.toResponse(gastoProgramadoService.crear(
                tiendaId, request.concepto(), request.monto(), request.frecuencia(), request.fechaInicio()));
        return ResponseEntity.ok(creado);
    }

    @PutMapping("/{id}")
    @RequiresPermission("GASTOS_PROGRAMADOS_EDITAR")
    public ResponseEntity<GastoProgramadoResponse> actualizar(
            @PathVariable Long tiendaId, @PathVariable Long id,
            @Valid @RequestBody ActualizarGastoProgramadoRequest request) {
        GastoProgramadoResponse actualizado = mapper.toResponse(gastoProgramadoService.actualizar(
                tiendaId, id, request.concepto(), request.monto(), request.frecuencia()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("GASTOS_PROGRAMADOS_EDITAR")
    public ResponseEntity<GastoProgramadoResponse> activar(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(gastoProgramadoService.activar(tiendaId, id)));
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("GASTOS_PROGRAMADOS_EDITAR")
    public ResponseEntity<GastoProgramadoResponse> desactivar(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(gastoProgramadoService.desactivar(tiendaId, id)));
    }

    @PostMapping("/{id}/generar-pago")
    @RequiresPermission("GASTOS_PROGRAMADOS_GENERAR_PAGO")
    public ResponseEntity<GastoProgramadoResponse> generarPago(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(gastoProgramadoService.generarPago(tiendaId, id)));
    }
}
