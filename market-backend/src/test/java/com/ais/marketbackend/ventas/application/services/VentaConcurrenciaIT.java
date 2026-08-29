package com.ais.marketbackend.ventas.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
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
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.exception.EstadoVentaInvalidoException;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
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
 * Fase 3 del plan (PLAN_MEJORAS.md, revisión general de flujos find-validar-save):
 * {@code VentaServiceImpl.completar}/{@code anular} leían la venta con
 * {@code findById} (sin bloqueo) — dos {@code completar} casi simultáneos sobre
 * la misma venta en BORRADOR (p. ej. un doble clic, o un reintento de red que se
 * solapa con el original) podían ambos leer BORRADOR y pasar la validación,
 * duplicando el movimiento de Inventario/CxC/Caja antes de que cualquiera
 * commiteara — mismo patrón ya corregido en Compra/Traslado/FEL, aquí en la
 * propia Venta. Ahora usa {@code findByIdConBloqueo} (`PESSIMISTIC_WRITE`).
 *
 * <p>Esta prueba corre dos {@code completar} realmente concurrentes contra
 * Postgres real (no mocks) sobre la misma venta: exactamente uno tiene éxito,
 * el inventario refleja la salida una sola vez.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class VentaConcurrenciaIT {

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
    private ClienteService clienteService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private UsuarioService usuarioService;

    private Long tiendaId;
    private Long vendedorId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-VENTA", "Grupo de prueba venta");
        TiendaResumen tienda = tiendaService.crear("T-VENTA", "Tienda de prueba venta", null, null, null, grupo.id());
        tiendaId = tienda.id();
        vendedorId = usuarioService.obtenerPorUsername("admin").id();
    }

    @Test
    void dosCompletarConcurrentesDeLaMismaVentaSoloUnoTieneExito() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria venta", null);
        MarcaResumen marca = marcaService.crear("Marca venta");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad venta", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-VENTA-" + System.nanoTime(), null, "Producto de prueba venta", null,
                categoria.id(), marca.id(), unidad.id(), null);
        productoTiendaService.asignar(
                producto.id(), tiendaId, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("500"),
                true, true);
        inventarioService.registrarMovimiento(
                tiendaId, producto.id(), new BigDecimal("100"), new BigDecimal("50.00"), TipoMovimiento.COMPRA);
        ClienteResumen cliente = clienteService.crear(
                null, "Cliente de prueba venta", null, null, null, new BigDecimal("1000.00"));

        VentaResumen venta = ventaService.crear(
                tiendaId, cliente.id(), vendedorId,
                List.of(new NuevaLineaVenta(producto.id(), new BigDecimal("10"), new BigDecimal("100.00"))),
                MetodoPago.CREDITO, null);

        List<Callable<Boolean>> tareas = List.of(
                () -> completarSiEsPosible(venta.id()), () -> completarSiEsPosible(venta.id()));
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
        assertThat(inventario.existenciaActual()).isEqualByComparingTo(new BigDecimal("90"));
    }

    private boolean completarSiEsPosible(Long ventaId) {
        try {
            ventaService.completar(tiendaId, ventaId);
            return true;
        } catch (EstadoVentaInvalidoException e) {
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
