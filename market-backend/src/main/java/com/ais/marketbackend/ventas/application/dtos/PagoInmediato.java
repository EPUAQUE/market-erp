package com.ais.marketbackend.ventas.application.dtos;

import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import java.math.BigDecimal;

/**
 * Un tramo de pago recibido en el momento de completar la venta (no un cobro
 * posterior contra una cuenta por cobrar) — solo tiene sentido un canal
 * concreto: {@code metodoPago} nunca es {@code CREDITO}/{@code MIXTO} aquí,
 * eso describe la venta completa, no un tramo. Ver
 * {@code VentaServiceImpl.completar}.
 */
public record PagoInmediato(MetodoPago metodoPago, BigDecimal monto) {
}
