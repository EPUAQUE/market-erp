package com.ais.marketbackend.fel.infrastructure.certificador;

import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import com.ais.marketbackend.fel.application.ports.ResultadoCertificacionFel;
import com.ais.marketbackend.fel.application.ports.SolicitudCertificacionFel;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Adaptador de desarrollo/pruebas: certifica localmente generando un UUID
 * aleatorio en vez de llamar a un proveedor certificador real autorizado por
 * la SAT. Reemplazar por un adaptador que integre con Infile/Digifact/G4S (u
 * otro proveedor) antes de emitir facturas reales — este adaptador nunca
 * produce un DTE fiscalmente válido.
 *
 * <p>Activo fuera de {@code prod} siempre, y dentro de {@code prod} solo si
 * {@code app.fel.requerido-real=false} (bandera temporal — ver
 * {@link FelSimuladoEnProdCondition} — mientras el cliente esté en fase de
 * pruebas sin facturación real, sin proveedor FEL contratado todavía).
 * {@link FelProdSafetyGuard} rechaza el arranque en {@code prod} si ese
 * puerto no está disponible ni la bandera lo permite.
 */
@Component
@Conditional(FelSimuladoEnProdCondition.class)
public class DevCertificadorFelAdapter implements CertificadorFelPort {

    @Override
    public ResultadoCertificacionFel certificar(SolicitudCertificacionFel solicitud) {
        return new ResultadoCertificacionFel(UUID.randomUUID().toString(), Instant.now());
    }
}
