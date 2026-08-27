package com.ais.marketbackend.dashboard.api.dtos.responses;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardGrupoResponse {

    Long grupoId;
    List<Long> tiendaIds;

    String ventasHoyTotal;
    long ventasHoyCantidad;
    String ventasMesTotal;
    long ventasMesCantidad;
    String ventasMesAnteriorTotal;
    String ticketPromedioMes;
    long facturasEmitidasMes;
    long facturasFelCertificadasMes;

    String utilidadMesTotal;
    String margenPromedioMes;

    String inventarioValorizadoTotal;
    long productosAgotados;
    long productosBajoMinimo;
    long productosSinMovimiento;

    String saldoPendienteCuentasPorCobrar;
    long cuentasPorCobrarVencidas;
    String cxcAging0a30;
    String cxcAging31a60;
    String cxcAgingMas60;

    String saldoPendienteCuentasPorPagar;
    long cuentasPorPagarVencidas;
    String cxpAging0a30;
    String cxpAging31a60;
    String cxpAgingMas60;

    long tiendasConCajaAbierta;
    long totalTiendas;
    String cajaSaldoEsperadoTotal;
    String ingresosHoy;
    String egresosHoy;

    long alertasCriticas;
    long alertasPreventivas;
}
