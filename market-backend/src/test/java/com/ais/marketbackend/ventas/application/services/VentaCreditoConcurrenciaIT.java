package com.ais.marketbackend.ventas.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
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
import com.ais.marketbackend.ventas.domain.exception.LimiteCreditoExcedidoException;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): antes, {@code validarLimiteCredito} leía el
 * cliente con {@code ClienteService.obtener} (sin bloqueo) — dos ventas a crédito
 * casi simultáneas del mismo cliente podían leer el mismo saldo pendiente (ninguna
 * veía la cuenta por cobrar que la otra estaba a punto de crear) y juntas exceder
 * el límite aunque cada una, evaluada sola, no lo hiciera. Ahora usa
 * {@code obtenerParaActualizarCredito}, que bloquea la fila del cliente con
 * {@code PESSIMISTIC_WRITE} (ver {@code ClienteRepository.findByIdConBloqueo}) hasta
 * que la transacción de {@code completar} termina, serializando la validación.
 *
 * <p>Esta prueba corre dos {@code completar()} realmente concurrentes contra
 * Postgres real (no mocks) para el mismo cliente, cada una por un monto que por sí
 * sola no excede el límite pero que juntas sí lo harían, y verifica que exactamente
 * una tiene éxito y el saldo pendiente final nunca supera el límite.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class VentaCreditoConcurrenciaIT {

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
    private CuentaPorCobrarService cuentaPorCobrarService;
    @Autowired
    private UsuarioService usuarioService;

    private Long tiendaId;
    private Long productoId;
    private Long vendedorId;

    @BeforeEach
    void setUp() {
        // Los servicios de dominio exigen un usuario autenticado (ContextoAutenticacionImpl) —
        // en producción lo pone el filtro JWT; aquí lo simulamos como el admin sembrado por
        // AdminUserSeeder (perfil test, seed.enabled=true), que tiene alcance global.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-CRED", "Grupo de prueba crédito");
        TiendaResumen tienda = tiendaService.crear("T-CRED", "Tienda de prueba crédito", null, null, null, grupo.id());
        tiendaId = tienda.id();

        CategoriaResumen categoria = categoriaService.crear("Categoria de prueba", null);
        MarcaResumen marca = marcaService.crear("Marca de prueba");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad de prueba", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-CRED-" + System.nanoTime(), null, "Producto de prueba crédito", null,
                categoria.id(), marca.id(), unidad.id(), null);
        productoId = producto.id();

        productoTiendaService.asignar(
                productoId, tiendaId, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000"), true, true);
        inventarioService.registrarMovimiento(
                tiendaId, productoId, new BigDecimal("100"), new BigDecimal("50.00"), TipoMovimiento.COMPRA);

        vendedorId = usuarioService.obtenerPorUsername("admin").id();
    }

    @Test
    void dosVentasACreditoCercanasAlLimiteNuncaLoSuperanJuntas() throws Exception {
        ClienteResumen cliente = clienteService.crear(
                null, "Cliente de prueba crédito", null, null, null, new BigDecimal("1000.00"));

        Long ventaAId = crearVentaCredito(cliente.id(), new BigDecimal("700.00"));
        Long ventaBId = crearVentaCredito(cliente.id(), new BigDecimal("700.00"));

        List<Callable<Boolean>> tareas = List.of(
                () -> completarSiEsPosible(ventaAId), () -> completarSiEsPosible(ventaBId));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        long exitosas = resultados.stream().filter(Boolean::booleanValue).count();
        assertThat(exitosas).isEqualTo(1);

        BigDecimal saldoTotal = cuentaPorCobrarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.clienteId().equals(cliente.id()))
                .map(CuentaPorCobrarResumen::saldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(saldoTotal).isLessThanOrEqualTo(new BigDecimal("1000.00"));
    }

    private Long crearVentaCredito(Long clienteId, BigDecimal monto) {
        VentaResumen venta = ventaService.crear(
                tiendaId, clienteId, vendedorId,
                List.of(new NuevaLineaVenta(productoId, BigDecimal.ONE, monto)), MetodoPago.CREDITO, null);
        return venta.id();
    }

    private boolean completarSiEsPosible(Long ventaId) {
        try {
            ventaService.completar(tiendaId, ventaId);
            return true;
        } catch (LimiteCreditoExcedidoException e) {
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
