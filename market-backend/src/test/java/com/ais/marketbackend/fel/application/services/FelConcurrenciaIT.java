package com.ais.marketbackend.fel.application.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
import com.ais.marketbackend.fel.domain.exception.EstadoDocumentoFelInvalidoException;
import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import com.ais.marketbackend.grupostienda.application.dtos.GrupoTiendaResumen;
import com.ais.marketbackend.grupostienda.application.services.interfaces.GrupoTiendaService;
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
 * Fase 3 del plan (PLAN_MEJORAS.md): antes, {@code FelServiceImpl.reintentar}/
 * {@code anular} leían el documento con {@code findById} (sin bloqueo) — dos
 * transiciones casi simultáneas sobre el mismo documento (p. ej. dos
 * {@code anular}) podían ambas leer el estado CERTIFICADO y pasar la
 * validación, dejando en BD solo el resultado de la que guardara al final sin
 * ningún error para la otra. Ahora usa {@code findByIdConBloqueo}
 * (`PESSIMISTIC_WRITE`).
 *
 * <p>Esta prueba corre dos {@code anular} realmente concurrentes contra
 * Postgres real (no mocks) sobre el mismo documento certificado: exactamente
 * uno tiene éxito, el otro recibe {@code EstadoDocumentoFelInvalidoException}.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class FelConcurrenciaIT {

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
    private com.ais.marketbackend.inventario.application.services.interfaces.InventarioService inventarioService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private FelService felService;
    @Autowired
    private UsuarioService usuarioService;

    private Long tiendaId;
    private Long vendedorId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        GrupoTiendaResumen grupo = grupoTiendaService.crear("G-FEL", "Grupo de prueba FEL");
        TiendaResumen tienda = tiendaService.crear("T-FEL", "Tienda de prueba FEL", null, null, null, grupo.id());
        tiendaId = tienda.id();
        vendedorId = usuarioService.obtenerPorUsername("admin").id();
    }

    @Test
    void dosAnulacionesConcurrentesDelMismoDocumentoSoloUnaTieneExito() throws Exception {
        CategoriaResumen categoria = categoriaService.crear("Categoria FEL", null);
        MarcaResumen marca = marcaService.crear("Marca FEL");
        UnidadMedidaResumen unidad = unidadMedidaService.crear("Unidad FEL", "u");
        ProductoResumen producto = productoService.crear(
                "SKU-FEL-" + System.nanoTime(), null, "Producto de prueba FEL", null,
                categoria.id(), marca.id(), unidad.id(), null);
        productoTiendaService.asignar(
                producto.id(), tiendaId, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("500"),
                true, true);
        inventarioService.registrarMovimiento(
                tiendaId, producto.id(), new BigDecimal("10"), new BigDecimal("50.00"),
                com.ais.marketbackend.inventario.domain.model.TipoMovimiento.COMPRA);
        ClienteResumen cliente = clienteService.crear(
                null, "Cliente de prueba FEL", null, null, null, new BigDecimal("1000.00"));

        VentaResumen venta = ventaService.crear(
                tiendaId, cliente.id(), vendedorId,
                List.of(new NuevaLineaVenta(producto.id(), BigDecimal.ONE, new BigDecimal("100.00"))),
                MetodoPago.CREDITO, null);
        ventaService.completar(tiendaId, venta.id());

        DocumentoFelResumen documento = felService.emitir(tiendaId, venta.id());
        assertThat(documento.estado()).isEqualTo(EstadoDocumentoFel.CERTIFICADO);

        List<Callable<Boolean>> tareas = List.of(
                () -> anularSiEsPosible(documento.id()), () -> anularSiEsPosible(documento.id()));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Boolean> resultados;
        try {
            List<Future<Boolean>> futuros = executor.invokeAll(tareas);
            resultados = futuros.stream().map(this::obtenerResultado).toList();
        } finally {
            executor.shutdown();
        }

        assertThat(resultados.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);

        DocumentoFelResumen actual = felService.obtener(tiendaId, documento.id());
        assertThat(actual.estado()).isEqualTo(EstadoDocumentoFel.ANULADO);
    }

    private boolean anularSiEsPosible(Long documentoId) {
        try {
            felService.anular(tiendaId, documentoId, "Prueba de concurrencia");
            return true;
        } catch (EstadoDocumentoFelInvalidoException e) {
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
