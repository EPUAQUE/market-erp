package com.ais.marketbackend.notificaciones.api.controllers;

import com.ais.marketbackend.notificaciones.api.dtos.responses.NotificacionResponse;
import com.ais.marketbackend.notificaciones.api.mappers.NotificacionApiMapper;
import com.ais.marketbackend.notificaciones.application.services.interfaces.NotificacionService;
import com.ais.marketbackend.seguridad.infrastructure.security.RequiresPermission;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificaciones/tiendas/{tiendaId}")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final NotificacionApiMapper mapper;

    @GetMapping
    @RequiresPermission("NOTIFICACIONES_VER")
    public ResponseEntity<List<NotificacionResponse>> listar(@PathVariable Long tiendaId) {
        List<NotificacionResponse> items =
                notificacionService.listarPorTienda(tiendaId).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/no-leidas")
    @RequiresPermission("NOTIFICACIONES_VER")
    public ResponseEntity<List<NotificacionResponse>> listarNoLeidas(@PathVariable Long tiendaId) {
        List<NotificacionResponse> items =
                notificacionService.listarNoLeidasPorTienda(tiendaId).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/generar")
    @RequiresPermission("NOTIFICACIONES_GENERAR")
    public ResponseEntity<List<NotificacionResponse>> generar(@PathVariable Long tiendaId) {
        List<NotificacionResponse> creadas =
                notificacionService.generar(tiendaId).stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(creadas);
    }

    @PostMapping("/{id}/marcar-leida")
    @RequiresPermission("NOTIFICACIONES_MARCAR_LEIDA")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long tiendaId, @PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(notificacionService.marcarLeida(tiendaId, id)));
    }
}
