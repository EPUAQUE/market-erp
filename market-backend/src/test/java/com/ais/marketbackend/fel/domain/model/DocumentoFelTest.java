package com.ais.marketbackend.fel.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ais.marketbackend.fel.domain.exception.EstadoDocumentoFelInvalidoException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DocumentoFelTest {

    @Test
    void nuevoEmpiezaPendienteSinUuid() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);

        assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoFel.PENDIENTE);
        assertThat(documento.getUuid()).isNull();
    }

    @Test
    void certificarDesdePendienteQuedaCertificado() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);

        documento.certificar("uuid-123", Instant.now());

        assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoFel.CERTIFICADO);
        assertThat(documento.getUuid()).isEqualTo("uuid-123");
    }

    @Test
    void marcarErrorDesdePendienteQuedaEnError() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);

        documento.marcarError("Timeout del certificador");

        assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoFel.ERROR);
        assertThat(documento.getMensajeError()).isEqualTo("Timeout del certificador");
    }

    @Test
    void certificarDesdeErrorReintentaCorrectamente() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);
        documento.marcarError("Timeout del certificador");

        documento.certificar("uuid-456", Instant.now());

        assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoFel.CERTIFICADO);
        assertThat(documento.getMensajeError()).isNull();
    }

    @Test
    void certificarUnDocumentoYaCertificadoLanzaEstadoInvalido() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);
        documento.certificar("uuid-123", Instant.now());

        assertThatThrownBy(() -> documento.certificar("uuid-789", Instant.now()))
                .isInstanceOf(EstadoDocumentoFelInvalidoException.class);
    }

    @Test
    void anularUnDocumentoCertificadoQuedaAnulado() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);
        documento.certificar("uuid-123", Instant.now());

        documento.anular("Error en los datos del cliente");

        assertThat(documento.getEstado()).isEqualTo(EstadoDocumentoFel.ANULADO);
        assertThat(documento.getMotivoAnulacion()).isEqualTo("Error en los datos del cliente");
    }

    @Test
    void anularUnDocumentoPendienteLanzaEstadoInvalido() {
        DocumentoFel documento = DocumentoFel.nuevo(1L, 2L, "A", 1L);

        assertThatThrownBy(() -> documento.anular("motivo")).isInstanceOf(EstadoDocumentoFelInvalidoException.class);
    }

    @Test
    void numeroCeroOMenorEsInvalido() {
        assertThatThrownBy(() -> DocumentoFel.nuevo(1L, 2L, "A", 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serieVaciaEsInvalida() {
        assertThatThrownBy(() -> DocumentoFel.nuevo(1L, 2L, " ", 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
