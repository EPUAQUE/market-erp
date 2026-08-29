package com.ais.marketbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
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
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fase 3 del plan (PLAN_MEJORAS.md): los {@code CHECK} de BD agregados en esta
 * fase (montos positivos, saldos no negativos, saldo &lt;= monto original,
 * stock_minimo &lt;= stock_maximo) son defensa en profundidad — el dominio ya
 * los exige, pero nada garantizaba que la restricción llegara realmente a
 * Postgres con la sintaxis correcta (el operador equivocado, una columna mal
 * escrita, etc. pasarían inadvertidos si solo se verifica que la migración
 * corre sin error). Esta prueba inserta filas inválidas directamente por SQL
 * (sin pasar por el dominio, que las rechazaría antes) contra Postgres real y
 * confirma que Postgres las rechaza — y que una fila válida sí se acepta.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CheckConstraintsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;
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
    private UsuarioService usuarioService;

    private Long tiendaId;
    private Long productoId;
    private Long ventaId;
    private Long clienteId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        String sufijo = String.valueOf(System.nanoTime() % 100_000_000L);
        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-CHK" + sufijo, "Grupo de prueba CHECK");
        TiendaResumen tienda =
                tiendaService.crear("T-CHK" + sufijo, "Tienda de prueba CHECK", null, null, null, grupo.id());
        tiendaId = tienda.id();

        CategoriaResumen categoria = categoriaService.crear("Categoria CHECK " + sufijo, null);
        MarcaResumen marca = marcaService.crear("Marca CHECK " + sufijo);
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad CHECK " + sufijo, "u");
        ProductoResumen producto = productoService.crear(
                "SKU-CHECK-" + System.nanoTime(), null, "Producto de prueba CHECK", null,
                categoria.id(), marca.id(), unidad.id(), null);
        productoId = producto.id();

        ClienteResumen cliente = clienteService.crear(null, "Cliente de prueba CHECK", null, null, null, null);
        clienteId = cliente.id();
        Long vendedorId = usuarioService.obtenerPorUsername("admin").id();
        VentaResumen venta = ventaService.crear(
                tiendaId, clienteId, vendedorId,
                List.of(new NuevaLineaVenta(productoId, BigDecimal.ONE, new BigDecimal("100.00"))),
                MetodoPago.CREDITO, null);
        ventaId = venta.id();
    }

    @Test
    void movimientoCajaConMontoNoPositivoEsRechazadoPorLaBaseDeDatos() {
        Long sesionId = insertarCajaSesionAbierta();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into movimiento_caja (caja_sesion_id, fecha, tipo, concepto, monto) "
                        + "values (?, now(), 'INGRESO', 'x', 0)",
                sesionId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_movimiento_caja_monto");

        jdbcTemplate.update(
                "insert into movimiento_caja (caja_sesion_id, fecha, tipo, concepto, monto) "
                        + "values (?, now(), 'INGRESO', 'x', 10)",
                sesionId);
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from movimiento_caja where caja_sesion_id = ?", Integer.class, sesionId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void cajaSesionConMontoFinalContadoNegativoEsRechazadaPorLaBaseDeDatos() {
        Long sesionId = insertarCajaSesionAbierta();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update caja_sesion set monto_final_contado = -1, estado = 'CERRADA', fecha_cierre = now() where id = ?",
                sesionId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_caja_sesion_monto_final_contado");
    }

    @Test
    void cuentaPorCobrarConSaldoMayorAlMontoOriginalEsRechazadaPorLaBaseDeDatos() {
        Long cuentaId = insertarCuentaPorCobrarPendiente(new BigDecimal("100.0000"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update cuenta_por_cobrar set saldo_pendiente = 150.0000 where id = ?", cuentaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_cuenta_por_cobrar_saldo_le_original");

        jdbcTemplate.update("update cuenta_por_cobrar set saldo_pendiente = 40.0000 where id = ?", cuentaId);
        BigDecimal saldo = jdbcTemplate.queryForObject(
                "select saldo_pendiente from cuenta_por_cobrar where id = ?", BigDecimal.class, cuentaId);
        assertThat(saldo).isEqualByComparingTo("40.0000");
    }

    @Test
    void productoTiendaConStockMinimoMayorAlMaximoEsRechazadoPorLaBaseDeDatos() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into producto_tienda "
                        + "(producto_id, tienda_id, precio_venta, stock_minimo, stock_maximo, permitir_venta, permitir_ingreso, activo) "
                        + "values (?, ?, 10.00, 500, 100, true, true, true)",
                productoId, tiendaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_producto_tienda_stock_min_le_max");
    }

    @Test
    void lineaVentaConCantidadNoPositivaEsRechazadaPorLaBaseDeDatos() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into linea_venta (venta_id, producto_id, cantidad, precio_unitario) values (?, ?, 0, 10.00)",
                ventaId, productoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_linea_venta_cantidad");
    }

    private Long insertarCajaSesionAbierta() {
        jdbcTemplate.update(
                "insert into caja_sesion (tienda_id, fecha_apertura, monto_inicial, estado) "
                        + "values (?, now(), 100.0000, 'ABIERTA')",
                tiendaId);
        return jdbcTemplate.queryForObject(
                "select id from caja_sesion where tienda_id = ? order by id desc limit 1", Long.class, tiendaId);
    }

    private Long insertarCuentaPorCobrarPendiente(BigDecimal montoOriginal) {
        jdbcTemplate.update(
                "insert into cuenta_por_cobrar "
                        + "(venta_id, cliente_id, tienda_id, fecha_emision, fecha_vencimiento, monto_original, saldo_pendiente, estado) "
                        + "values (?, ?, ?, now(), now() + interval '30 days', ?, ?, 'PENDIENTE')",
                ventaId, clienteId, tiendaId, montoOriginal, montoOriginal);
        return jdbcTemplate.queryForObject(
                "select id from cuenta_por_cobrar where venta_id = ?", Long.class, ventaId);
    }
}
