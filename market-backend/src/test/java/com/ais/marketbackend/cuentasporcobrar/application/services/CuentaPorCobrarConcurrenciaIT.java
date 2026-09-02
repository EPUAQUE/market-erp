package com.ais.marketbackend.cuentasporcobrar.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.exception.CobroExcedeSaldoException;
import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.tiendas.application.dtos.TiendaResumen;
import com.ais.marketbackend.tiendas.application.services.interfaces.TiendaService;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): antes,
 * {@code CuentaPorCobrarServiceImpl.registrarCobro} leía la cuenta con
 * {@code findById} (sin bloqueo) — dos cobros casi simultáneos sobre la misma
 * cuenta podían leer el mismo saldo pendiente y juntas superarlo aunque cada
 * uno, evaluado solo, no lo hiciera (y la colección JPA de cobros, con
 * {@code orphanRemoval}, podía perder uno de los dos en un merge concurrente
 * sin lock). Ahora usa {@code findByIdConBloqueo} (`PESSIMISTIC_WRITE`),
 * serializando la validación.
 *
 * <p>Esta prueba corre dos {@code registrarCobro} realmente concurrentes
 * contra Postgres real (no mocks) para la misma cuenta, cada uno por un monto
 * que por sí solo no excede el saldo pero que juntos sí lo harían.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CuentaPorCobrarConcurrenciaIT {

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
    private ClienteService clienteService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private CuentaPorCobrarService cuentaPorCobrarService;
    @Autowired
    private UsuarioService usuarioService;

    private Long tiendaId;
    private Long vendedorId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-CXC", "Grupo de prueba CxC");
        TiendaResumen tienda = tiendaService.crear("T-CXC", "Tienda de prueba CxC", null, null, null, grupo.id());
        tiendaId = tienda.id();
        vendedorId = usuarioService.obtenerPorUsername("admin").id();
    }

    @Test
    void dosCobrosConcurrentesCercanosAlSaldoNuncaLoSuperanNiLoNegativizan() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria CxC", null);
        MarcaResumen marca = marcaService.crear("Marca CxC");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad CxC", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-CXC-" + System.nanoTime(), null, "Producto de prueba CxC", null, null,
                categoria.id(), marca.id(), unidad.id(), null);
        ClienteResumen cliente = clienteService.crear(null, "Cliente de prueba CxC", null, null, null, null);
        VentaResumen venta = ventaService.crear(
                tiendaId, cliente.id(), vendedorId,
                List.of(new NuevaLineaVenta(producto.id(), BigDecimal.ONE, new BigDecimal("100.00"))),
                com.ais.marketbackend.ventas.domain.model.MetodoPago.CREDITO, null);

        CuentaPorCobrarResumen cuenta =
                cuentaPorCobrarService.crear(venta.id(), cliente.id(), tiendaId, new BigDecimal("100.00"));

        List<Callable<Boolean>> tareas = List.of(
                () -> cobrarSiEsPosible(cuenta.id()), () -> cobrarSiEsPosible(cuenta.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        CuentaPorCobrarResumen actual = cuentaPorCobrarService.obtener(tiendaId, cuenta.id());
        assertThat(actual.saldoPendiente()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(actual.saldoPendiente()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    private boolean cobrarSiEsPosible(Long cuentaId) {
        try {
            cuentaPorCobrarService.registrarCobro(tiendaId, cuentaId, new BigDecimal("60.00"), MetodoPago.EFECTIVO);
            return true;
        } catch (CobroExcedeSaldoException e) {
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
