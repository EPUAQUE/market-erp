package com.ais.marketbackend.productos.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.categorias.domain.model.EstadoCategoria;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.marcas.domain.model.EstadoMarca;
import com.ais.marketbackend.productos.application.dtos.ImportacionFilaResultado;
import com.ais.marketbackend.productos.application.dtos.ProductoResumen;
import com.ais.marketbackend.productos.application.services.impl.ProductoImportacionServiceImpl;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.domain.exception.ArchivoImportacionInvalidoException;
import com.ais.marketbackend.productos.domain.exception.ProductoDuplicadoException;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import com.ais.marketbackend.unidadesmedida.domain.model.EstadoUnidadMedida;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Usa el .xlsx real que el cliente entregó como formato de carga masiva
 * (copiado a src/test/resources/productos) — así el test cubre el formato
 * real acordado, no un fixture inventado a mano.
 */
class ProductoImportacionServiceImplTest {

    private ProductoService productoService;
    private CategoriaService categoriaService;
    private MarcaService marcaService;
    private UnidadMedidaService unidadMedidaService;
    private ProductoImportacionServiceImpl importacionService;

    private final AtomicLong siguienteIdCategoria = new AtomicLong(100);
    private final AtomicLong siguienteIdMarca = new AtomicLong(200);
    private final AtomicLong siguienteIdUnidad = new AtomicLong(300);
    private final AtomicLong siguienteIdProducto = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        productoService = mock(ProductoService.class);
        categoriaService = mock(CategoriaService.class);
        marcaService = mock(MarcaService.class);
        unidadMedidaService = mock(UnidadMedidaService.class);
        importacionService = new ProductoImportacionServiceImpl(
                productoService, categoriaService, marcaService, unidadMedidaService);

        when(categoriaService.listar()).thenReturn(List.of());
        when(marcaService.listar()).thenReturn(List.of());
        when(unidadMedidaService.listar()).thenReturn(List.of());

        when(categoriaService.crear(anyString(), any())).thenAnswer(inv -> new CategoriaResumen(
                siguienteIdCategoria.getAndIncrement(), inv.getArgument(0), null, EstadoCategoria.ACTIVA));
        when(marcaService.crear(anyString())).thenAnswer(inv -> new MarcaResumen(
                siguienteIdMarca.getAndIncrement(), inv.getArgument(0), EstadoMarca.ACTIVA));
        when(unidadMedidaService.crear(anyString(), anyString())).thenAnswer(inv -> new UnidadMedidaResumen(
                siguienteIdUnidad.getAndIncrement(), inv.getArgument(0), inv.getArgument(1),
                EstadoUnidadMedida.ACTIVA));

        when(productoService.crear(
                        anyString(), any(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new ProductoResumen(
                        siguienteIdProducto.getAndIncrement(), inv.getArgument(0), inv.getArgument(1),
                        inv.getArgument(2), inv.getArgument(3), inv.getArgument(4), inv.getArgument(5),
                        inv.getArgument(6), inv.getArgument(7), inv.getArgument(8), true));
    }

    private InputStream archivoCliente() throws IOException {
        Path ruta = Path.of("src/test/resources/productos/codigosBarra_completado.xlsx");
        return new ByteArrayInputStream(Files.readAllBytes(ruta));
    }

    @Test
    void importaLas12FilasDelExcelRealDelClienteSinErrores() throws IOException {
        List<ImportacionFilaResultado> resultados = importacionService.importar(archivoCliente());

        assertThat(resultados).hasSize(12);
        assertThat(resultados).allMatch(ImportacionFilaResultado::creado);
        assertThat(resultados.get(0).codigoInterno()).isEqualTo("PRD-0001");
        assertThat(resultados.get(11).codigoInterno()).isEqualTo("PRD-0012");
    }

    @Test
    void creaCadaCategoriaMarcaYUnidadSoloUnaVezAunqueSeRepitanEnVariasFilas() throws IOException {
        importacionService.importar(archivoCliente());

        // La hoja "Categorías"/"Marcas"/"Unidades de medida" del propio archivo confirma
        // el universo esperado: 5 categorías, 6 marcas, 2 unidades (L, ml) — pese a que
        // "Axion"/"Lavatrastes"/"ml" se repiten en varias de las 12 filas de productos.
        verify(categoriaService, times(5)).crear(anyString(), eq(null));
        verify(marcaService, times(6)).crear(anyString());
        verify(unidadMedidaService, times(2)).crear(anyString(), anyString());
        verify(productoService, times(12)).crear(
                anyString(), any(), anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unaFilaConCodigoInternoDuplicadoNoDetieneElRestoDeLaImportacion() throws IOException {
        when(productoService.crear(
                        eq("PRD-0005"), any(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ProductoDuplicadoException("PRD-0005"));

        List<ImportacionFilaResultado> resultados = importacionService.importar(archivoCliente());

        assertThat(resultados).hasSize(12);
        assertThat(resultados).filteredOn(r -> !r.creado()).hasSize(1);
        ImportacionFilaResultado fallida = resultados.stream().filter(r -> !r.creado()).findFirst().orElseThrow();
        assertThat(fallida.codigoInterno()).isEqualTo("PRD-0005");
        assertThat(fallida.mensaje()).contains("PRD-0005");
        assertThat(resultados.stream().filter(ImportacionFilaResultado::creado)).hasSize(11);
    }

    @Test
    void unArchivoQueNoEsExcelLanzaExcepcionDeNegocio() {
        InputStream archivoInvalido = new ByteArrayInputStream("no es un excel".getBytes());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> importacionService.importar(archivoInvalido)))
                .isInstanceOf(ArchivoImportacionInvalidoException.class);
    }

    @Test
    void reutilizaCategoriaMarcaYUnidadYaExistentesEnVezDeCrearDuplicados() throws IOException {
        when(categoriaService.listar()).thenReturn(List.of(
                new CategoriaResumen(1L, "Detergentes para ropa", null, EstadoCategoria.ACTIVA),
                new CategoriaResumen(2L, "Cloros y blanqueadores", null, EstadoCategoria.ACTIVA),
                new CategoriaResumen(3L, "Lavatrastes", null, EstadoCategoria.ACTIVA),
                new CategoriaResumen(4L, "Suavizantes", null, EstadoCategoria.ACTIVA),
                new CategoriaResumen(5L, "Desinfectantes", null, EstadoCategoria.ACTIVA)));
        when(marcaService.listar()).thenReturn(List.of(
                new MarcaResumen(10L, "Más Color", EstadoMarca.ACTIVA),
                new MarcaResumen(11L, "Member's Selection", EstadoMarca.ACTIVA),
                new MarcaResumen(12L, "Ariel", EstadoMarca.ACTIVA),
                new MarcaResumen(13L, "Magia Blanca", EstadoMarca.ACTIVA),
                new MarcaResumen(14L, "Axion", EstadoMarca.ACTIVA),
                new MarcaResumen(15L, "Azistin", EstadoMarca.ACTIVA)));
        when(unidadMedidaService.listar()).thenReturn(List.of(
                new UnidadMedidaResumen(20L, "L", "L", EstadoUnidadMedida.ACTIVA),
                new UnidadMedidaResumen(21L, "ml", "ml", EstadoUnidadMedida.ACTIVA)));

        importacionService.importar(archivoCliente());

        verify(categoriaService, never()).crear(anyString(), any());
        verify(marcaService, never()).crear(anyString());
        verify(unidadMedidaService, never()).crear(anyString(), anyString());
        verify(productoService).crear(
                eq("PRD-0001"), any(), anyString(), any(), any(), eq(1L), eq(10L), eq(20L), any());
    }
}
