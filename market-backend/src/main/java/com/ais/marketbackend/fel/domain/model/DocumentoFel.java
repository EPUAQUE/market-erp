package com.ais.marketbackend.fel.domain.model;

import com.ais.marketbackend.fel.domain.exception.EstadoDocumentoFelInvalidoException;
import java.time.Instant;
import java.util.Objects;

/**
 * Representa la Factura Electrónica en Línea (FEL, régimen de Guatemala) de una
 * {@code Venta} (otro módulo — ver {@code FelServiceImpl.emitir}). La
 * certificación real ante la SAT la realiza un proveedor certificador externo
 * (Infile, Digifact, G4S, etc.) a través de {@code CertificadorFelPort} — este
 * agregado solo modela el ciclo de vida del documento, no el detalle fiscal
 * (XML del DTE, firma, etc.), que es responsabilidad del proveedor.
 */
public class DocumentoFel {

    private final Long id;
    private final Long ventaId;
    private final Long tiendaId;
    private final String serie;
    private final long numero;
    private String uuid;
    private EstadoDocumentoFel estado;
    private final Instant fechaEmision;
    private Instant fechaCertificacion;
    private String motivoAnulacion;
    private String mensajeError;

    public DocumentoFel(
            Long id, Long ventaId, Long tiendaId, String serie, long numero, String uuid,
            EstadoDocumentoFel estado, Instant fechaEmision, Instant fechaCertificacion, String motivoAnulacion,
            String mensajeError) {
        this.id = id;
        this.ventaId = Objects.requireNonNull(ventaId, "ventaId");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.serie = requerirSerie(serie);
        if (numero <= 0) {
            throw new IllegalArgumentException("El número debe ser mayor que cero.");
        }
        this.numero = numero;
        this.uuid = uuid;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.fechaEmision = Objects.requireNonNull(fechaEmision, "fechaEmision");
        this.fechaCertificacion = fechaCertificacion;
        this.motivoAnulacion = motivoAnulacion;
        this.mensajeError = mensajeError;
    }

    public static DocumentoFel nuevo(Long ventaId, Long tiendaId, String serie, long numero) {
        return new DocumentoFel(
                null, ventaId, tiendaId, serie, numero, null, EstadoDocumentoFel.PENDIENTE, Instant.now(), null,
                null, null);
    }

    /** Llamado tras una respuesta exitosa del certificador. Válido desde PENDIENTE o ERROR (reintento). */
    public void certificar(String uuid, Instant fechaCertificacion) {
        exigirCertificable();
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.fechaCertificacion = Objects.requireNonNull(fechaCertificacion, "fechaCertificacion");
        this.mensajeError = null;
        this.estado = EstadoDocumentoFel.CERTIFICADO;
    }

    /** Llamado cuando el certificador rechaza o no responde. El documento queda apto para reintentar. */
    public void marcarError(String mensaje) {
        exigirCertificable();
        this.mensajeError = mensaje;
        this.estado = EstadoDocumentoFel.ERROR;
    }

    public void anular(String motivo) {
        if (estado != EstadoDocumentoFel.CERTIFICADO) {
            throw new EstadoDocumentoFelInvalidoException(estado);
        }
        this.motivoAnulacion = motivo;
        this.estado = EstadoDocumentoFel.ANULADO;
    }

    private void exigirCertificable() {
        if (estado != EstadoDocumentoFel.PENDIENTE && estado != EstadoDocumentoFel.ERROR) {
            throw new EstadoDocumentoFelInvalidoException(estado);
        }
    }

    private static String requerirSerie(String serie) {
        if (serie == null || serie.isBlank()) {
            throw new IllegalArgumentException("La serie no puede estar vacía.");
        }
        return serie;
    }

    public Long getId() {
        return id;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public String getSerie() {
        return serie;
    }

    public long getNumero() {
        return numero;
    }

    public String getUuid() {
        return uuid;
    }

    public EstadoDocumentoFel getEstado() {
        return estado;
    }

    public Instant getFechaEmision() {
        return fechaEmision;
    }

    public Instant getFechaCertificacion() {
        return fechaCertificacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public String getMensajeError() {
        return mensajeError;
    }
}
