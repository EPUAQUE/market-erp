package com.ais.marketbackend.productos.application.services.interfaces;

import com.ais.marketbackend.productos.application.dtos.ImportacionFilaResultado;
import java.io.InputStream;
import java.util.List;

public interface ProductoImportacionService {

    /**
     * Lee un .xlsx con la hoja "Productos" (o la primera hoja si no existe con ese
     * nombre) y crea un producto por fila. Categoría, marca y unidad de medida se
     * resuelven por nombre y se crean automáticamente si no existen todavía.
     */
    List<ImportacionFilaResultado> importar(InputStream archivo);
}
