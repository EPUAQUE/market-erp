package com.ais.marketbackend.clientes.api.controllers;

import com.ais.marketbackend.clientes.api.dtos.requests.ActualizarClienteRequest;
import com.ais.marketbackend.clientes.api.dtos.requests.CrearClienteRequest;
import com.ais.marketbackend.clientes.api.dtos.responses.ClienteResponse;
import com.ais.marketbackend.clientes.api.mappers.ClienteApiMapper;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
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

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteApiMapper mapper;

    @GetMapping
    @RequiresPermission("CLIENTES_VER")
    public ResponseEntity<PaginaResponse<ClienteResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PaginacionParams.TAMANO_DEFECTO) int size) {
        var pagina = clienteService.listar(
                PaginacionParams.normalizarPagina(page), PaginacionParams.normalizarTamano(size));
        return ResponseEntity.ok(PaginaResponse.de(pagina, mapper::toResponse));
    }

    @PostMapping
    @RequiresPermission("CLIENTES_CREAR")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest request) {
        ClienteResponse creado = mapper.toResponse(clienteService.crear(
                request.nit(), request.nombre(), request.direccion(), request.telefono(), request.correo(),
                request.limiteCredito(), request.correlationId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Fase 2 (PLAN_MEJORAS.md): resuelve "Consumidor Final" por nombre, nunca por
     * el id fijo `1` que antes asumía {@code market-flutter} — la primera fila
     * insertada en una BD nueva es un accidente de orden de migración, no un
     * contrato. Antes de {@code /{id}} en el código a propósito (aunque Spring ya
     * prioriza segmentos literales sobre variables de path sin importar el orden
     * de declaración, mismo patrón ya usado en {@code CajaController}/{@code
     * CuentaPorCobrarController}).
     */
    @GetMapping("/consumidor-final")
    @RequiresPermission("CLIENTES_VER")
    public ResponseEntity<ClienteResponse> obtenerConsumidorFinal() {
        return ResponseEntity.ok(mapper.toResponse(clienteService.obtenerConsumidorFinal()));
    }

    @PutMapping("/{id}")
    @RequiresPermission("CLIENTES_EDITAR")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ActualizarClienteRequest request) {
        ClienteResponse actualizado = mapper.toResponse(clienteService.actualizar(
                id, request.nombre(), request.direccion(), request.telefono(), request.correo(),
                request.limiteCredito()));
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/activar")
    @RequiresPermission("CLIENTES_EDITAR")
    public ResponseEntity<Void> activar(@PathVariable Long id) {
        clienteService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @RequiresPermission("CLIENTES_EDITAR")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        clienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
