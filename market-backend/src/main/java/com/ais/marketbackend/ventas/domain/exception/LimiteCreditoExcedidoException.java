package com.ais.marketbackend.ventas.domain.exception;

import com.ais.marketbackend.shared.exceptions.BusinessException;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;

/**
 * Se lanza al completar una venta a crédito cuyo saldo pendiente proyectado
 * (lo que el cliente ya debe en esta tienda + el total de esta venta)
 * superaría {@code Cliente.limiteCredito}. Solo aplica cuando el cliente
 * tiene un límite definido — {@code null} significa sin restricción (ver
 * CLAUDE.md de market-flutter, "Known backend gaps").
 */
public class LimiteCreditoExcedidoException extends BusinessException {

    public LimiteCreditoExcedidoException(Long clienteId, BigDecimal limite, BigDecimal saldoProyectado) {
        super("La venta a crédito excede el límite de crédito del cliente " + clienteId + " (límite: "
                + limite.toPlainString() + ", saldo proyectado: " + saldoProyectado.toPlainString() + ").");
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String errorCode() {
        return "LIMITE_CREDITO_EXCEDIDO";
    }
}
