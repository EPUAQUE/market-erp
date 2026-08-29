package com.ais.marketbackend.compras.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.compras.domain.exception.EstadoCompraInvalidoException;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoTiendaService;
import com.ais.marketbackend.proveedores.application.dtos.ProveedorResumen;
import com.ais.marketbackend.proveedores.application.services.interfaces.ProveedorService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): antes, {@code CompraServiceImpl.recibir}/
 * {@code anular} leían la compra con {@code findById} (sin bloqueo) — dos
 * transiciones casi simultáneas sobre la misma compra (p. ej. dos
 * {@code recibir}) podían ambas leer el estado BORRADOR y pasar la
 * validación, duplicando el movimiento de Inventario antes de que la
 * restricción única de {@code cuenta_por_pagar.compra_id} abortara la
 * transacción perdedora completa (incluido el movimiento de inventario ya
 * insertado en esa misma transacción) — dejando un mensaje de error engañoso
 * ("proveedor/tienda no existe"). Ahora usa {@code findByIdConBloqueo}
 * (`PESSIMISTIC_WRITE`): la segunda solicitud espera, relee el estado ya
 * actualizado y falla con el error correcto ({@code EstadoCompraInvalidoException}).
 *
 * <p>Esta prueba corre dos {@code recibir} realmente concurrentes contra
 * Postgres real (no mocks) sobre la misma compra: exactamente uno tiene
 * éxito, el inventario refleja la recepción una sola vez.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CompraConcurrenciaIT {

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
    private ProveedorService proveedorService;
    @Autowired
    private CompraService compraService;
    @Autowired
    private InventarioService inventarioService;

    private Long tiendaId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-COMPRA", "Grupo de prueba compra");
        TiendaResumen tienda =
                tiendaService.crear("T-COMPRA", "Tienda de prueba compra", null, null, null, grupo.id());
        tiendaId = tienda.id();
    }

    @Test
    void dosRecepcionesConcurrentesDeLaMismaCompraSoloUnaTieneExito() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria compra", null);
        MarcaResumen marca = marcaService.crear("Marca compra");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad compra", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-COMPRA-" + System.nanoTime(), null, "Producto de prueba compra", null,
                categoria.id(), marca.id(), unidad.id(), null);
        ProveedorResumen proveedor = proveedorService.crear(
                "NIT" + (System.nanoTime() % 100_000_000L), "Proveedor de prueba compra", null, null, null);
        productoTiendaService.asignar(
                producto.id(), tiendaId, new BigDecimal("12.50"), BigDecimal.ZERO, new BigDecimal("500"), true, true);

        CompraResumen compra = compraService.crear(
                tiendaId, proveedor.id(),
                List.of(new NuevaLineaCompra(producto.id(), new BigDecimal("10"), new BigDecimal("5.00"))));

        List<Callable<Boolean>> tareas = List.of(
                () -> recibirSiEsPosible(compra.id()), () -> recibirSiEsPosible(compra.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        InventarioResumen inventario = inventarioService.obtener(tiendaId, producto.id());
        assertThat(inventario.existenciaActual()).isEqualByComparingTo(new BigDecimal("10"));
    }

    private boolean recibirSiEsPosible(Long compraId) {
        try {
            compraService.recibir(tiendaId, compraId);
            return true;
        } catch (EstadoCompraInvalidoException e) {
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
