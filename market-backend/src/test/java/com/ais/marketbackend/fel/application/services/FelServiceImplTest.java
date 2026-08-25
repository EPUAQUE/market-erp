package com.ais.marketbackend.fel.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import com.ais.marketbackend.fel.application.ports.ResultadoCertificacionFel;
import com.ais.marketbackend.fel.application.services.impl.FelServiceImpl;
import com.ais.marketbackend.fel.domain.exception.CertificacionFallidaException;
import com.ais.marketbackend.fel.domain.exception.VentaNoCompletadaException;
import com.ais.marketbackend.fel.domain.exception.VentaYaFacturadaException;
import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import com.ais.marketbackend.fel.domain.model.EstadoDocumentoFel;
import com.ais.marketbackend.fel.domain.repository.DocumentoFelRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FelServiceImplTest {

    private DocumentoFelRepository documentoFelRepository;
    private VentaService ventaService;
    private CertificadorFelPort certificadorFelPort;
    private FelServiceImpl felService;

    @BeforeEach
    void setUp() {
        documentoFelRepository = mock(DocumentoFelRepository.class);
        ventaService = mock(VentaService.class);
        certificadorFelPort = mock(CertificadorFelPort.class);
        felService = new FelServiceImpl(documentoFelRepository, ventaService, certificadorFelPort);

        when(documentoFelRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentoFelRepository.findByVentaId(9L)).thenReturn(Optional.empty());
        when(documentoFelRepository.siguienteNumero(1L, "A")).thenReturn(1L);
        when(ventaService.obtener(1L, 9L)).thenReturn(venta(EstadoVenta.COMPLETADA));
    }

    @Test
    void emitirCertificaExitosamenteYQuedaCertificado() {
        when(certificadorFelPort.certificar(any()))
                .thenReturn(new ResultadoCertificacionFel("uuid-abc", Instant.now()));

        DocumentoFelResumen resumen = felService.emitir(1L, 9L);

        assertThat(resumen.estado()).isEqualTo(EstadoDocumentoFel.CERTIFICADO);
        assertThat(resumen.uuid()).isEqualTo("uuid-abc");
        assertThat(resumen.numero()).isEqualTo(1L);
    }

    @Test
    void emitirConCertificadorQueFallaQuedaEnError() {
        when(certificadorFelPort.certificar(any())).thenThrow(new CertificacionFallidaException("SAT no disponible"));

        DocumentoFelResumen resumen = felService.emitir(1L, 9L);

        assertThat(resumen.estado()).isEqualTo(EstadoDocumentoFel.ERROR);
        assertThat(resumen.mensajeError()).isEqualTo("SAT no disponible");
    }

    @Test
    void emitirParaVentaNoCompletadaLanzaExcepcion() {
        when(ventaService.obtener(1L, 9L)).thenReturn(venta(EstadoVenta.BORRADOR));

        assertThatThrownBy(() -> felService.emitir(1L, 9L)).isInstanceOf(VentaNoCompletadaException.class);
    }

    @Test
    void emitirParaVentaYaFacturadaLanzaExcepcion() {
        DocumentoFel existente = DocumentoFel.nuevo(9L, 1L, "A", 1L);
        when(documentoFelRepository.findByVentaId(9L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> felService.emitir(1L, 9L)).isInstanceOf(VentaYaFacturadaException.class);
    }

    @Test
    void reintentarSobreUnDocumentoEnErrorLoCertifica() {
        DocumentoFel documento = withId(DocumentoFel.nuevo(9L, 1L, "A", 1L), 5L);
        documento.marcarError("Timeout");
        when(documentoFelRepository.findById(5L)).thenReturn(Optional.of(documento));
        when(certificadorFelPort.certificar(any()))
                .thenReturn(new ResultadoCertificacionFel("uuid-retry", Instant.now()));

        DocumentoFelResumen resumen = felService.reintentar(1L, 5L);

        assertThat(resumen.estado()).isEqualTo(EstadoDocumentoFel.CERTIFICADO);
        assertThat(resumen.uuid()).isEqualTo("uuid-retry");
    }

    @Test
    void reintentarDeOtraTiendaLanzaNoEncontrado() {
        DocumentoFel documento = withId(DocumentoFel.nuevo(9L, 1L, "A", 1L), 5L);
        documento.marcarError("Timeout");
        when(documentoFelRepository.findById(5L)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> felService.reintentar(99L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anularUnDocumentoCertificadoQuedaAnulado() {
        DocumentoFel documento = withId(DocumentoFel.nuevo(9L, 1L, "A", 1L), 5L);
        documento.certificar("uuid-abc", Instant.now());
        when(documentoFelRepository.findById(5L)).thenReturn(Optional.of(documento));

        DocumentoFelResumen resumen = felService.anular(1L, 5L, "Cliente solicitó anulación");

        assertThat(resumen.estado()).isEqualTo(EstadoDocumentoFel.ANULADO);
        assertThat(resumen.motivoAnulacion()).isEqualTo("Cliente solicitó anulación");
    }

    @Test
    void emitirEnviaAlCertificadorLosDatosFiscalesDeLaVenta() {
        when(certificadorFelPort.certificar(any()))
                .thenReturn(new ResultadoCertificacionFel("uuid-abc", Instant.now()));

        felService.emitir(1L, 9L);

        verify(certificadorFelPort).certificar(org.mockito.ArgumentMatchers.argThat(
                solicitud -> solicitud.clienteId().equals(3L)
                        && solicitud.total().compareTo(new BigDecimal("100.00")) == 0
                        && solicitud.serie().equals("A")
                        && solicitud.numero() == 1L));
    }

    private VentaResumen venta(EstadoVenta estado) {
        return new VentaResumen(
                9L, 3L, 1L, 4L, Instant.now(), estado, List.of(), new BigDecimal("100.00"), MetodoPago.EFECTIVO);
    }

    private DocumentoFel withId(DocumentoFel documento, Long id) {
        return new DocumentoFel(
                id, documento.getVentaId(), documento.getTiendaId(), documento.getSerie(), documento.getNumero(),
                documento.getUuid(), documento.getEstado(), documento.getFechaEmision(),
                documento.getFechaCertificacion(), documento.getMotivoAnulacion(), documento.getMensajeError());
    }
}
