package com.ais.marketbackend.fel.application.services.impl;

import com.ais.marketbackend.auditoria.infrastructure.aop.Auditable;
import com.ais.marketbackend.fel.application.dtos.DocumentoFelResumen;
import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import com.ais.marketbackend.fel.application.ports.ResultadoCertificacionFel;
import com.ais.marketbackend.fel.application.ports.SolicitudCertificacionFel;
import com.ais.marketbackend.fel.application.services.interfaces.FelService;
import com.ais.marketbackend.fel.domain.exception.CertificacionFallidaException;
import com.ais.marketbackend.fel.domain.exception.VentaNoCompletadaException;
import com.ais.marketbackend.fel.domain.exception.VentaYaFacturadaException;
import com.ais.marketbackend.fel.domain.model.DocumentoFel;
import com.ais.marketbackend.fel.domain.repository.DocumentoFelRepository;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.model.EstadoVenta;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code ventaService} es una dependencia cruzada de módulo permitida: solo se
 * usa su puerto {@code application.services.interfaces}, para leer los datos
 * fiscales de la venta (cliente, total, fecha) que viajan en la solicitud de
 * certificación. {@code certificadorFelPort} es la frontera hacia el proveedor
 * certificador externo — ver su Javadoc.
 */
@Service
public class FelServiceImpl implements FelService {

    private static final String SERIE_DEFAULT = "A";

    private final DocumentoFelRepository documentoFelRepository;
    private final VentaService ventaService;
    private final CertificadorFelPort certificadorFelPort;

    public FelServiceImpl(
            DocumentoFelRepository documentoFelRepository, VentaService ventaService,
            CertificadorFelPort certificadorFelPort) {
        this.documentoFelRepository = documentoFelRepository;
        this.ventaService = ventaService;
        this.certificadorFelPort = certificadorFelPort;
    }

    @Override
    @Transactional
    @Auditable(accion = "FEL_EMITIDO", entidad = "DOCUMENTO_FEL", tiendaIdParam = "tiendaId", entidadIdFromReturn = true)
    public DocumentoFelResumen emitir(Long tiendaId, Long ventaId) {
        VentaResumen venta = ventaService.obtener(tiendaId, ventaId);
        if (venta.estado() != EstadoVenta.COMPLETADA) {
            throw new VentaNoCompletadaException();
        }
        if (documentoFelRepository.findByVentaId(ventaId).isPresent()) {
            throw new VentaYaFacturadaException();
        }
        long numero = documentoFelRepository.siguienteNumero(tiendaId, SERIE_DEFAULT);
        DocumentoFel documento =
                documentoFelRepository.save(DocumentoFel.nuevo(ventaId, tiendaId, SERIE_DEFAULT, numero));
        return toResumen(certificar(documento, venta));
    }

    @Override
    @Transactional
    @Auditable(accion = "FEL_REINTENTADO", entidad = "DOCUMENTO_FEL", tiendaIdParam = "tiendaId", entidadIdParam = "id")
    public DocumentoFelResumen reintentar(Long tiendaId, Long id) {
        DocumentoFel documento = obtenerConBloqueoORequerido(tiendaId, id);
        VentaResumen venta = ventaService.obtener(tiendaId, documento.getVentaId());
        return toResumen(certificar(documento, venta));
    }

    @Override
    @Transactional
    @Auditable(accion = "FEL_ANULADO", entidad = "DOCUMENTO_FEL", tiendaIdParam = "tiendaId", entidadIdParam = "id")
    public DocumentoFelResumen anular(Long tiendaId, Long id, String motivo) {
        DocumentoFel documento = obtenerConBloqueoORequerido(tiendaId, id);
        documento.anular(motivo);
        return toResumen(documentoFelRepository.save(documento));
    }

    @Override
    public DocumentoFelResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerido(tiendaId, id));
    }

    @Override
    public List<DocumentoFelResumen> listarPorTienda(Long tiendaId) {
        return documentoFelRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    private DocumentoFel certificar(DocumentoFel documento, VentaResumen venta) {
        try {
            ResultadoCertificacionFel resultado = certificadorFelPort.certificar(new SolicitudCertificacionFel(
                    documento.getTiendaId(), documento.getSerie(), documento.getNumero(), venta.clienteId(),
                    venta.total(), venta.fecha()));
            documento.certificar(resultado.uuid(), resultado.fechaCertificacion());
        } catch (CertificacionFallidaException e) {
            documento.marcarError(e.getMessage());
        }
        return documentoFelRepository.save(documento);
    }

    private DocumentoFel obtenerORequerido(Long tiendaId, Long id) {
        DocumentoFel documento = documentoFelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento FEL no encontrado: " + id));
        if (!documento.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Documento FEL no encontrado: " + id);
        }
        return documento;
    }

    /** Igual que {@link #obtenerORequerido}, pero con {@code findByIdConBloqueo} — ver {@code DocumentoFelRepository}. */
    private DocumentoFel obtenerConBloqueoORequerido(Long tiendaId, Long id) {
        DocumentoFel documento = documentoFelRepository.findByIdConBloqueo(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento FEL no encontrado: " + id));
        if (!documento.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Documento FEL no encontrado: " + id);
        }
        return documento;
    }

    private DocumentoFelResumen toResumen(DocumentoFel documento) {
        return new DocumentoFelResumen(
                documento.getId(), documento.getVentaId(), documento.getTiendaId(), documento.getSerie(),
                documento.getNumero(), documento.getUuid(), documento.getEstado(), documento.getFechaEmision(),
                documento.getFechaCertificacion(), documento.getMotivoAnulacion(), documento.getMensajeError());
    }
}
