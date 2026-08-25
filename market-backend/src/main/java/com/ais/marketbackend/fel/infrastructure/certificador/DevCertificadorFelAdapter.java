package com.ais.marketbackend.fel.infrastructure.certificador;

import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import com.ais.marketbackend.fel.application.ports.ResultadoCertificacionFel;
import com.ais.marketbackend.fel.application.ports.SolicitudCertificacionFel;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de desarrollo/pruebas: certifica localmente generando un UUID
 * aleatorio en vez de llamar a un proveedor certificador real autorizado por
 * la SAT. Reemplazar por un adaptador que integre con Infile/Digifact/G4S (u
 * otro proveedor) antes de operar en producción — este adaptador nunca
 * produce un DTE fiscalmente válido.
 */
@Component
public class DevCertificadorFelAdapter implements CertificadorFelPort {

    @Override
    public ResultadoCertificacionFel certificar(SolicitudCertificacionFel solicitud) {
        return new ResultadoCertificacionFel(UUID.randomUUID().toString(), Instant.now());
    }
}
