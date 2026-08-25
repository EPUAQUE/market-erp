package com.ais.marketbackend.fel.application.ports;

import com.ais.marketbackend.fel.domain.exception.CertificacionFallidaException;

/**
 * Puerto hacia un proveedor certificador autorizado por la SAT (régimen FEL de
 * Guatemala — p. ej. Infile, Digifact, G4S). La implementación real generaría
 * el XML del DTE, lo firmaría y lo enviaría al certificador; ninguno de esos
 * detalles pertenece a este módulo, que solo orquesta el ciclo de vida del
 * documento. Ver {@code DevCertificadorFelAdapter} para el adaptador de
 * desarrollo usado mientras no hay una integración real configurada.
 */
public interface CertificadorFelPort {

    ResultadoCertificacionFel certificar(SolicitudCertificacionFel solicitud) throws CertificacionFallidaException;
}
