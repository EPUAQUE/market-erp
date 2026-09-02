package com.ais.marketbackend.traslados.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.traslados.application.dtos.NuevaLineaTraslado;
import com.ais.marketbackend.traslados.application.dtos.TrasladoResumen;
import com.ais.marketbackend.traslados.application.services.interfaces.TrasladoService;
import com.ais.marketbackend.traslados.domain.exception.EstadoTrasladoInvalidoException;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 3 del plan (PLAN_MEJORAS.md): antes, {@code TrasladoServiceImpl.completar}/
 * {@code anular} leían el traslado con {@code findById} (sin bloqueo) — dos
 * {@code completar} casi simultáneos sobre el mismo traslado podían ambos leer
 * el estado BORRADOR y pasar la validación, registrando el movimiento de
 * salida/entrada en Inventario DOS VECES — sin ninguna restricción de BD que
 * lo impidiera (a diferencia de Compra/FEL, aquí no hay una cuenta por pagar
 * ni un documento con clave única que aborte la segunda transacción). Ahora
 * usa {@code findByIdConBloqueo} (`PESSIMISTIC_WRITE`).
 *
 * <p>Esta prueba corre dos {@code completar} realmente concurrentes contra
 * Postgres real (no mocks) sobre el mismo traslado: exactamente uno tiene
 * éxito, el inventario de destino refleja la entrada una sola vez.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class TrasladoConcurrenciaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private GrupoTiendaService grupoTiendaService;
    @Autowired
    private TiendaService tiendaService;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private MarcaService marcaService;
    @Autowired
    private UnidadMedidaService unidadMedidaService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private ProductoTiendaService productoTiendaService;
    @Autowired
    private InventarioService inventarioService;
    @Autowired
    private TrasladoService trasladoService;

    private Long tiendaOrigenId;
    private Long tiendaDestinoId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-TRASLADO", "Grupo de prueba traslado");
        TiendaResumen origen = tiendaService.crear(
                "T-TRAS-O", "Tienda origen traslado", null, null, null, grupo.id());
        TiendaResumen destino = tiendaService.crear(
                "T-TRAS-D", "Tienda destino traslado", null, null, null, grupo.id());
        tiendaOrigenId = origen.id();
        tiendaDestinoId = destino.id();
    }

    @Test
    void dosCompletarConcurrentesDelMismoTrasladoSoloUnoTieneExito() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria traslado", null);
        MarcaResumen marca = marcaService.crear("Marca traslado");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad traslado", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-TRAS-" + System.nanoTime(), null, "Producto de prueba traslado", null, null,
                categoria.id(), marca.id(), unidad.id(), null);

        productoTiendaService.asignar(
                producto.id(), tiendaOrigenId, new BigDecimal("12.50"), BigDecimal.ZERO, new BigDecimal("500"),
                true, true);
        productoTiendaService.asignar(
                producto.id(), tiendaDestinoId, new BigDecimal("12.50"), BigDecimal.ZERO, new BigDecimal("500"),
                true, true);
        inventarioService.registrarMovimiento(
                tiendaOrigenId, producto.id(), new BigDecimal("100"), new BigDecimal("5.00"), TipoMovimiento.COMPRA);

        TrasladoResumen traslado = trasladoService.crear(
                tiendaOrigenId, tiendaDestinoId, List.of(new NuevaLineaTraslado(producto.id(), new BigDecimal("10"))));

        List<Callable<Boolean>> tareas = List.of(
                () -> completarSiEsPosible(traslado.id()), () -> completarSiEsPosible(traslado.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        InventarioResumen destino = inventarioService.obtener(tiendaDestinoId, producto.id());
        assertThat(destino.existenciaActual()).isEqualByComparingTo(new BigDecimal("10"));
        InventarioResumen origen = inventarioService.obtener(tiendaOrigenId, producto.id());
        assertThat(origen.existenciaActual()).isEqualByComparingTo(new BigDecimal("90"));
    }

    private boolean completarSiEsPosible(Long trasladoId) {
        // SecurityContextHolder es ThreadLocal por defecto: no se propaga a los hilos
        // del ExecutorService, y TrasladoServiceImpl.completar exige acceso vía
        // AutorizacionTiendaService — hay que autenticar cada hilo por separado.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
        try {
            trasladoService.completar(trasladoId);
            return true;
        } catch (EstadoTrasladoInvalidoException e) {
            return false;
        }
    }

    private Boolean obtenerResultado(Future<Boolean> futuro) {
        try {
            return futuro.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
