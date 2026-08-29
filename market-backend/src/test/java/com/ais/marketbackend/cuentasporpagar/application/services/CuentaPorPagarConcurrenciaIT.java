package com.ais.marketbackend.cuentasporpagar.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.compras.application.dtos.CompraResumen;
import com.ais.marketbackend.compras.application.dtos.NuevaLineaCompra;
import com.ais.marketbackend.compras.application.services.interfaces.CompraService;
import com.ais.marketbackend.cuentasporpagar.application.dtos.CuentaPorPagarResumen;
import com.ais.marketbackend.cuentasporpagar.application.services.interfaces.CuentaPorPagarService;
import com.ais.marketbackend.cuentasporpagar.domain.exception.PagoExcedeSaldoException;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): mismo motivo y misma solución que
 * {@code CuentaPorCobrarConcurrenciaIT}, aplicado a
 * {@code CuentaPorPagarServiceImpl.registrarPago} (ahora usa
 * {@code findByIdConBloqueo}, `PESSIMISTIC_WRITE`).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CuentaPorPagarConcurrenciaIT {

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
    private ProveedorService proveedorService;
    @Autowired
    private CompraService compraService;
    @Autowired
    private CuentaPorPagarService cuentaPorPagarService;

    private Long tiendaId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-CXP", "Grupo de prueba CxP");
        TiendaResumen tienda = tiendaService.crear("T-CXP", "Tienda de prueba CxP", null, null, null, grupo.id());
        tiendaId = tienda.id();
    }

    @Test
    void dosPagosConcurrentesCercanosAlSaldoNuncaLoSuperanNiLoNegativizan() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria CxP", null);
        MarcaResumen marca = marcaService.crear("Marca CxP");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad CxP", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-CXP-" + System.nanoTime(), null, "Producto de prueba CxP", null,
                categoria.id(), marca.id(), unidad.id(), null);
        ProveedorResumen proveedor = proveedorService.crear(
                "NIT" + (System.nanoTime() % 100_000_000L), "Proveedor de prueba CxP", null, null, null);
        CompraResumen compra = compraService.crear(
                tiendaId, proveedor.id(),
                List.of(new NuevaLineaCompra(producto.id(), BigDecimal.ONE, new BigDecimal("100.00"))));

        CuentaPorPagarResumen cuenta =
                cuentaPorPagarService.crear(compra.id(), proveedor.id(), tiendaId, new BigDecimal("100.00"));

        List<Callable<Boolean>> tareas = List.of(
                () -> pagarSiEsPosible(cuenta.id()), () -> pagarSiEsPosible(cuenta.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        CuentaPorPagarResumen actual = cuentaPorPagarService.obtener(tiendaId, cuenta.id());
        assertThat(actual.saldoPendiente()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(actual.saldoPendiente()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    private boolean pagarSiEsPosible(Long cuentaId) {
        try {
            cuentaPorPagarService.registrarPago(tiendaId, cuentaId, new BigDecimal("60.00"));
            return true;
        } catch (PagoExcedeSaldoException e) {
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
