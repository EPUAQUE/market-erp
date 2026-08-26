package com.ais.marketbackend.productos.infrastructure.storage;

import com.ais.marketbackend.productos.domain.exception.ImagenInvalidaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda imágenes de producto en disco (volumen Docker en prod, ver
 * docker-compose.yml) y devuelve una URL relativa servida por
 * {@link ImagenesWebConfig} bajo {@code /api/v1/productos/imagenes/**} — nunca el
 * nombre original del archivo, para no depender de la entrada del cliente al armar
 * la ruta.
 */
@Service
public class ImagenProductoAlmacenamientoService {

    private static final String RUTA_PUBLICA = "/api/v1/productos/imagenes/";
    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path directorio;

    public ImagenProductoAlmacenamientoService(
            @Value("${app.storage.productos-imagenes-dir}") String directorioConfigurado) {
        this.directorio = Path.of(directorioConfigurado).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ImagenInvalidaException("El archivo de imagen está vacío.");
        }
        String extension = EXTENSIONES_PERMITIDAS.get(archivo.getContentType());
        if (extension == null) {
            throw new ImagenInvalidaException("Formato no permitido — use JPG, PNG o WEBP.");
        }
        try {
            Files.createDirectories(directorio);
            String nombreArchivo = UUID.randomUUID() + "." + extension;
            archivo.transferTo(directorio.resolve(nombreArchivo));
            return RUTA_PUBLICA + nombreArchivo;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen del producto.", e);
        }
    }
}
