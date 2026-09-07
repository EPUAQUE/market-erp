package com.ais.marketbackend.productos.application.services.impl;

import com.ais.marketbackend.categorias.application.dtos.CategoriaResumen;
import com.ais.marketbackend.categorias.application.services.interfaces.CategoriaService;
import com.ais.marketbackend.marcas.application.dtos.MarcaResumen;
import com.ais.marketbackend.marcas.application.services.interfaces.MarcaService;
import com.ais.marketbackend.productos.application.dtos.ImportacionFilaResultado;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoImportacionService;
import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.domain.exception.ArchivoImportacionInvalidoException;
import com.ais.marketbackend.shared.exceptions.BusinessException;
import com.ais.marketbackend.unidadesmedida.application.dtos.UnidadMedidaResumen;
import com.ais.marketbackend.unidadesmedida.application.services.interfaces.UnidadMedidaService;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

/**
 * Carga masiva de productos desde un .xlsx (formato acordado con el cliente:
 * hoja "Productos" con columnas Código interno, Código de barras, Nombre,
 * Descripción, Descripción corta, Categoría, Marca, Unidad de medida — las
 * columnas Estado código y Fuente son metadatos del cliente y se ignoran).
 * Cada fila se procesa en su propia transacción (delegada a los servicios
 * inyectados, cada uno @Transactional): una fila con error no revierte las
 * anteriores.
 */
@Service
public class ProductoImportacionServiceImpl implements ProductoImportacionService {

    private static final String NOMBRE_HOJA_PRODUCTOS = "productos";
    private static final DataFormatter FORMATEADOR = new DataFormatter();

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final MarcaService marcaService;
    private final UnidadMedidaService unidadMedidaService;

    public ProductoImportacionServiceImpl(
            ProductoService productoService, CategoriaService categoriaService, MarcaService marcaService,
            UnidadMedidaService unidadMedidaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.marcaService = marcaService;
        this.unidadMedidaService = unidadMedidaService;
    }

    @Override
    public List<ImportacionFilaResultado> importar(InputStream archivo) {
        Sheet hoja = leerHojaProductos(archivo);
        Map<String, Integer> columnas = leerColumnas(hoja);

        Map<String, Long> categoriasPorNombre = new HashMap<>();
        for (CategoriaResumen c : categoriaService.listar()) {
            categoriasPorNombre.put(normalizar(c.nombre()), c.id());
        }
        Map<String, Long> marcasPorNombre = new HashMap<>();
        for (MarcaResumen m : marcaService.listar()) {
            marcasPorNombre.put(normalizar(m.nombre()), m.id());
        }
        Map<String, Long> unidadesPorNombre = new HashMap<>();
        for (UnidadMedidaResumen u : unidadMedidaService.listar()) {
            unidadesPorNombre.put(normalizar(u.nombre()), u.id());
            unidadesPorNombre.put(normalizar(u.abreviacion()), u.id());
        }

        List<ImportacionFilaResultado> resultados = new ArrayList<>();
        int primeraFilaDatos = hoja.getFirstRowNum() + 1;
        int ultimaFila = hoja.getLastRowNum();
        for (int i = primeraFilaDatos; i <= ultimaFila; i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue;
            }
            int numeroFilaExcel = i + 1;
            String codigoInterno = valor(fila, columnas.get("codigo interno"));
            String codigoBarras = valor(fila, columnas.get("codigo de barras"));
            String nombre = valor(fila, columnas.get("nombre"));
            String descripcion = valor(fila, columnas.get("descripcion"));
            String descripcionCorta = valor(fila, columnas.get("descripcion corta"));
            String categoriaNombre = valor(fila, columnas.get("categoria"));
            String marcaNombre = valor(fila, columnas.get("marca"));
            String unidadNombre = valor(fila, columnas.get("unidad de medida"));

            if (esFilaVacia(codigoInterno, codigoBarras, nombre, categoriaNombre, marcaNombre, unidadNombre)) {
                continue;
            }

            resultados.add(procesarFila(
                    numeroFilaExcel, codigoInterno, codigoBarras, nombre, descripcion, descripcionCorta,
                    categoriaNombre, marcaNombre, unidadNombre, categoriasPorNombre, marcasPorNombre,
                    unidadesPorNombre));
        }

        if (resultados.isEmpty()) {
            throw new ArchivoImportacionInvalidoException("El archivo no tiene filas de productos para importar.");
        }
        return resultados;
    }

    private ImportacionFilaResultado procesarFila(
            int numeroFilaExcel, String codigoInterno, String codigoBarras, String nombre, String descripcion,
            String descripcionCorta, String categoriaNombre, String marcaNombre, String unidadNombre,
            Map<String, Long> categoriasPorNombre, Map<String, Long> marcasPorNombre,
            Map<String, Long> unidadesPorNombre) {
        try {
            String error = validarCampos(codigoInterno, nombre, codigoBarras, descripcion, descripcionCorta,
                    categoriaNombre, marcaNombre, unidadNombre);
            if (error != null) {
                return new ImportacionFilaResultado(numeroFilaExcel, codigoInterno, false, error);
            }

            Long categoriaId = resolverCategoria(categoriaNombre, categoriasPorNombre);
            Long marcaId = resolverMarca(marcaNombre, marcasPorNombre);
            Long unidadMedidaId = resolverUnidadMedida(unidadNombre, unidadesPorNombre);

            productoService.crear(
                    codigoInterno, blankToNull(codigoBarras), nombre, blankToNull(descripcion),
                    blankToNull(descripcionCorta), categoriaId, marcaId, unidadMedidaId, null);
            return new ImportacionFilaResultado(numeroFilaExcel, codigoInterno, true, "Creado correctamente.");
        } catch (BusinessException e) {
            return new ImportacionFilaResultado(numeroFilaExcel, codigoInterno, false, e.getMessage());
        } catch (RuntimeException e) {
            return new ImportacionFilaResultado(
                    numeroFilaExcel, codigoInterno, false, "Error inesperado: " + e.getMessage());
        }
    }

    private String validarCampos(
            String codigoInterno, String nombre, String codigoBarras, String descripcion, String descripcionCorta,
            String categoriaNombre, String marcaNombre, String unidadNombre) {
        if (esVacio(codigoInterno)) {
            return "El código interno es obligatorio.";
        }
        if (codigoInterno.length() > 40) {
            return "El código interno no puede superar 40 caracteres.";
        }
        if (!esVacio(codigoBarras) && codigoBarras.length() > 40) {
            return "El código de barras no puede superar 40 caracteres.";
        }
        if (esVacio(nombre)) {
            return "El nombre es obligatorio.";
        }
        if (nombre.length() > 150) {
            return "El nombre no puede superar 150 caracteres.";
        }
        if (!esVacio(descripcion) && descripcion.length() > 1000) {
            return "La descripción no puede superar 1000 caracteres.";
        }
        if (!esVacio(descripcionCorta) && descripcionCorta.length() > 100) {
            return "La descripción corta no puede superar 100 caracteres.";
        }
        if (esVacio(categoriaNombre)) {
            return "La categoría es obligatoria.";
        }
        if (esVacio(marcaNombre)) {
            return "La marca es obligatoria.";
        }
        if (esVacio(unidadNombre)) {
            return "La unidad de medida es obligatoria.";
        }
        return null;
    }

    private Long resolverCategoria(String nombre, Map<String, Long> cache) {
        String clave = normalizar(nombre);
        Long id = cache.get(clave);
        if (id != null) {
            return id;
        }
        CategoriaResumen creada = categoriaService.crear(nombre.trim(), null);
        cache.put(clave, creada.id());
        return creada.id();
    }

    private Long resolverMarca(String nombre, Map<String, Long> cache) {
        String clave = normalizar(nombre);
        Long id = cache.get(clave);
        if (id != null) {
            return id;
        }
        MarcaResumen creada = marcaService.crear(nombre.trim());
        cache.put(clave, creada.id());
        return creada.id();
    }

    private Long resolverUnidadMedida(String nombre, Map<String, Long> cache) {
        String clave = normalizar(nombre);
        Long id = cache.get(clave);
        if (id != null) {
            return id;
        }
        String valorTrim = nombre.trim();
        UnidadMedidaResumen creada = unidadMedidaService.crear(valorTrim, valorTrim);
        cache.put(clave, creada.id());
        return creada.id();
    }

    private Sheet leerHojaProductos(InputStream archivo) {
        Workbook libro;
        try {
            libro = WorkbookFactory.create(archivo);
        } catch (IOException | RuntimeException e) {
            throw new ArchivoImportacionInvalidoException("El archivo no es un Excel (.xlsx) válido.");
        }
        for (Sheet hoja : libro) {
            if (normalizar(hoja.getSheetName()).equals(NOMBRE_HOJA_PRODUCTOS)) {
                return hoja;
            }
        }
        Sheet primera = libro.getSheetAt(0);
        if (primera == null) {
            throw new ArchivoImportacionInvalidoException("El archivo no tiene ninguna hoja.");
        }
        return primera;
    }

    private Map<String, Integer> leerColumnas(Sheet hoja) {
        Row encabezado = hoja.getRow(hoja.getFirstRowNum());
        if (encabezado == null) {
            throw new ArchivoImportacionInvalidoException("El archivo no tiene fila de encabezado.");
        }
        Map<String, Integer> columnas = new HashMap<>();
        for (Cell celda : encabezado) {
            String texto = normalizar(FORMATEADOR.formatCellValue(celda));
            if (!texto.isEmpty()) {
                columnas.put(texto, celda.getColumnIndex());
            }
        }
        String[] requeridas = {
            "codigo interno", "nombre", "categoria", "marca", "unidad de medida"
        };
        for (String requerida : requeridas) {
            if (!columnas.containsKey(requerida)) {
                throw new ArchivoImportacionInvalidoException(
                        "Falta la columna obligatoria '" + requerida + "' en el encabezado.");
            }
        }
        return columnas;
    }

    private String valor(Row fila, Integer columna) {
        if (columna == null) {
            return "";
        }
        Cell celda = fila.getCell(columna);
        if (celda == null) {
            return "";
        }
        return FORMATEADOR.formatCellValue(celda).trim();
    }

    private boolean esFilaVacia(String... valores) {
        for (String valor : valores) {
            if (!esVacio(valor)) {
                return false;
            }
        }
        return true;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String blankToNull(String valor) {
        return esVacio(valor) ? null : valor;
    }

    /** minúsculas, sin acentos, espacios colapsados — para comparar encabezados y nombres de catálogo. */
    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
