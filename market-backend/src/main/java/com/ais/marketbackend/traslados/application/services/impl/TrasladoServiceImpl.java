package com.ais.marketbackend.traslados.application.services.impl;

import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.seguridad.application.services.interfaces.AutorizacionTiendaService;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.traslados.application.dtos.LineaTrasladoResumen;
import com.ais.marketbackend.traslados.application.dtos.NuevaLineaTraslado;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import com.ais.marketbackend.traslados.application.services.interfaces.TrasladoService;
import com.ais.marketbackend.traslados.domain.model.LineaTraslado;
import com.ais.marketbackend.traslados.domain.model.Traslado;
import com.ais.marketbackend.traslados.domain.repository.TrasladoRepository;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code inventarioService} es una dependencia cruzada de módulo permitida:
 * solo se usa su puerto {@code application.services.interfaces}. {@code completar}
 * registra, por cada línea, un TRASLADO_SALIDA en la tienda de origen y un
 * TRASLADO_ENTRADA en la de destino, dentro de la misma transacción que el
 * cambio de estado — si cualquiera de los dos falla (p. ej. stock insuficiente
 * en origen o {@code permitirIngreso=false} en destino), todo se revierte y el
 * traslado permanece en BORRADOR.
 *
 * <p>El costo registrado en ambos movimientos es el costo promedio vigente en
 * la tienda de <b>origen</b> — el costo del producto viaja con él, en vez de
 * mezclarse prematuramente con el costo ya existente en destino.
 *
 * <p>Un traslado involucra dos tiendas, así que el alcance de tienda no se puede
 * anclar a una única variable de ruta como hace {@code PermissionInterceptor} —
 * este servicio valida explícitamente ambas tiendas vía
 * {@code AutorizacionTiendaService} en cada operación.
 *
 * <p>{@code obtener}/{@code completar}/{@code anular} reciben el {@code id} del
 * traslado, no un {@code tiendaId} elegido por quien llama — si primero se
 * confirma que el traslado existe ({@code obtenerORequerido}) y luego el
 * acceso fuera de alcance responde {@code 403}, alguien probando ids
 * consecutivos puede distinguir "no existe" (404) de "existe pero no es mío"
 * (403), filtrando qué ids son traslados reales de otras tiendas. Por eso esas
 * tres operaciones traducen la denegación a {@code ResourceNotFoundException}
 * (mismo 404 que un id inexistente) vía {@code exigirAccesoOFingirNoEncontrado}.
 * {@code crear} no tiene este problema — el {@code tiendaId} lo elige quien
 * llama, no lo adivina — así que ahí la denegación sigue siendo un {@code 403}
 * informativo.
 */
@Service
public class TrasladoServiceImpl implements TrasladoService {

    private final TrasladoRepository trasladoRepository;
    private final InventarioService inventarioService;
    private final AutorizacionTiendaService autorizacionTiendaService;

    public TrasladoServiceImpl(
            TrasladoRepository trasladoRepository, InventarioService inventarioService,
            AutorizacionTiendaService autorizacionTiendaService) {
        this.trasladoRepository = trasladoRepository;
        this.inventarioService = inventarioService;
        this.autorizacionTiendaService = autorizacionTiendaService;
    }

    @Override
    @Transactional
    public TrasladoResumen crear(Long tiendaOrigenId, Long tiendaDestinoId, List<NuevaLineaTraslado> lineas) {
        autorizacionTiendaService.exigirAccesoATodas(List.of(tiendaOrigenId, tiendaDestinoId));
        List<LineaTraslado> lineasDominio = lineas.stream()
                .map(l -> LineaTraslado.nueva(l.productoId(), l.cantidad()))
                .toList();
        Traslado traslado = Traslado.nuevo(tiendaOrigenId, tiendaDestinoId, lineasDominio);
        return toResumen(trasladoRepository.save(traslado));
    }

    @Override
    @Transactional
    public TrasladoResumen completar(Long id) {
        Traslado traslado = obtenerORequerido(id);
        exigirAccesoOFingirNoEncontrado(id, traslado);
        traslado.completar();
        for (LineaTraslado linea : traslado.getLineas()) {
            InventarioResumen origen = inventarioService.obtener(traslado.getTiendaOrigenId(), linea.getProductoId());
            inventarioService.registrarMovimiento(
                    traslado.getTiendaOrigenId(), linea.getProductoId(), linea.getCantidad(),
                    origen.costoPromedioActual(), TipoMovimiento.TRASLADO_SALIDA);
            inventarioService.registrarMovimiento(
                    traslado.getTiendaDestinoId(), linea.getProductoId(), linea.getCantidad(),
                    origen.costoPromedioActual(), TipoMovimiento.TRASLADO_ENTRADA);
        }
        return toResumen(trasladoRepository.save(traslado));
    }

    @Override
    @Transactional
    public TrasladoResumen anular(Long id) {
        Traslado traslado = obtenerORequerido(id);
        exigirAccesoOFingirNoEncontrado(id, traslado);
        traslado.anular();
        return toResumen(trasladoRepository.save(traslado));
    }

    @Override
    public TrasladoResumen obtener(Long id) {
        Traslado traslado = obtenerORequerido(id);
        exigirAccesoOFingirNoEncontrado(id, traslado);
        return toResumen(traslado);
    }

    @Override
    public List<TrasladoResumen> listar() {
        return trasladoRepository.findAll().stream()
                .filter(t -> autorizacionTiendaService.tieneAcceso(t.getTiendaOrigenId())
                        && autorizacionTiendaService.tieneAcceso(t.getTiendaDestinoId()))
                .map(this::toResumen)
                .toList();
    }

    @Override
    public Pagina<TrasladoResumen> listar(int pagina, int tamano) {
        var tiendaIdsPermitidas = autorizacionTiendaService.tiendaIdsPermitidas();
        Pagina<Traslado> resultado = tiendaIdsPermitidas.isEmpty()
                ? trasladoRepository.listar(pagina, tamano)
                : trasladoRepository.listarPorTiendas(tiendaIdsPermitidas.get(), pagina, tamano);
        return resultado.map(this::toResumen);
    }

    private Traslado obtenerORequerido(Long id) {
        return trasladoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traslado no encontrado: " + id));
    }

    private void exigirAccesoOFingirNoEncontrado(Long id, Traslado traslado) {
        try {
            autorizacionTiendaService.exigirAccesoATodas(
                    List.of(traslado.getTiendaOrigenId(), traslado.getTiendaDestinoId()));
        } catch (AccessDeniedException e) {
            throw new ResourceNotFoundException("Traslado no encontrado: " + id);
        }
    }

    private TrasladoResumen toResumen(Traslado traslado) {
        List<LineaTrasladoResumen> lineas = traslado.getLineas().stream()
                .map(l -> new LineaTrasladoResumen(l.getId(), l.getProductoId(), l.getCantidad()))
                .toList();
        return new TrasladoResumen(
                traslado.getId(), traslado.getTiendaOrigenId(), traslado.getTiendaDestinoId(), traslado.getFecha(),
                traslado.getEstado(), lineas);
    }
}
