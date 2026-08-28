package com.ais.marketbackend.fel.infrastructure.certificador;

import com.ais.marketbackend.fel.application.ports.CertificadorFelPort;
import com.ais.marketbackend.fel.application.ports.ResultadoCertificacionFel;
import com.ais.marketbackend.fel.application.ports.SolicitudCertificacionFel;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador de desarrollo/pruebas: certifica localmente generando un UUID
 * aleatorio en vez de llamar a un proveedor certificador real autorizado por
 * la SAT. Reemplazar por un adaptador que integre con Infile/Digifact/G4S (u
 * otro proveedor) antes de operar en producción — este adaptador nunca
 * produce un DTE fiscalmente válido.
 *
 * <p>Restringido a {@code @Profile("!prod")}: en producción este bean no se
 * registra, así que solo puede existir un {@link CertificadorFelPort} activo si
 * se implementó y configuró uno real. {@link FelProdSafetyGuard} además rechaza
 * el arranque en {@code prod} si ese puerto no está disponible.
 */
@Component
@Profile("!prod")
public class DevCertificadorFelAdapter implements CertificadorFelPort {

    @Override
    public ResultadoCertificacionFel certificar(SolicitudCertificacionFel solicitud) {
        return new ResultadoCertificacionFel(UUID.randomUUID().toString(), Instant.now());
    }
}
