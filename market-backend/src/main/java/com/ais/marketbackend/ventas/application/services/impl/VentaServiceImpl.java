package com.ais.marketbackend.ventas.application.services.impl;

import com.ais.marketbackend.caja.application.services.interfaces.CajaService;
import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
import com.ais.marketbackend.clientes.application.dtos.ClienteResumen;
import com.ais.marketbackend.clientes.application.services.interfaces.ClienteService;
import com.ais.marketbackend.cuentasporcobrar.application.dtos.CuentaPorCobrarResumen;
import com.ais.marketbackend.cuentasporcobrar.application.services.interfaces.CuentaPorCobrarService;
import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import com.ais.marketbackend.inventario.application.dtos.InventarioResumen;
import com.ais.marketbackend.inventario.application.services.interfaces.InventarioService;
import com.ais.marketbackend.inventario.domain.model.TipoMovimiento;
import com.ais.marketbackend.shared.domain.Pagina;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.ventas.application.dtos.LineaVentaResumen;
import com.ais.marketbackend.ventas.application.dtos.NuevaLineaVenta;
import com.ais.marketbackend.ventas.application.dtos.PagoInmediato;
import com.ais.marketbackend.ventas.application.dtos.VentaResumen;
import com.ais.marketbackend.ventas.application.services.interfaces.VentaService;
import com.ais.marketbackend.ventas.domain.exception.CajaNoAbiertaException;
import com.ais.marketbackend.ventas.domain.exception.CorrelationIdReutilizadoException;
import com.ais.marketbackend.ventas.domain.exception.DesglosePagoInvalidoException;
import com.ais.marketbackend.ventas.domain.exception.LimiteCreditoExcedidoException;
import com.ais.marketbackend.ventas.domain.exception.ReferenciaInvalidaException;
import com.ais.marketbackend.ventas.domain.model.LineaVenta;
import com.ais.marketbackend.ventas.domain.model.MetodoPago;
import com.ais.marketbackend.ventas.domain.model.Venta;
import com.ais.marketbackend.ventas.domain.repository.VentaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code inventarioService}/{@code cuentaPorCobrarService}/{@code clienteService}/
 * {@code cajaService} son dependencias cruzadas de módulo permitidas: solo se usan
 * sus puertos {@code application.services.interfaces}. {@code completar} resuelve
 * el pago inmediato según {@code Venta.metodoPago} (ver
 * {@code resolverPagosInmediatos}), exige una caja abierta para cualquier
 * método salvo CREDITO (si no, {@code CajaNoAbiertaException} — antes de esto
 * la venta se completaba igual y el ingreso se descartaba en silencio), valida
 * el límite de crédito sobre el saldo
 * financiado real (si la venta es a crédito o mixta — ver
 * {@code validarLimiteCredito}), registra un movimiento VENTA por línea, refleja
 * cada pago inmediato como ingreso en Caja, y crea una cuenta por cobrar
 * únicamente por el saldo no cubierto (nunca para EFECTIVO/TARJETA/TRANSFERENCIA,
 * que siempre cubren el total de inmediato) — todo dentro de la misma transacción
 * que el cambio de estado: si cualquier paso falla (p. ej. Inventario rechaza una
 * línea, o el límite de crédito se excede), toda la operación se revierte y la
 * venta permanece en BORRADOR.
 *
 * <p>El costo registrado en el kardex de cada movimiento VENTA es el costo
 * promedio vigente en Inventario en ese momento (COGS) — no el precio cobrado
 * al cliente, que vive únicamente en {@code LineaVenta.precioUnitario}.
 */
@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final InventarioService inventarioService;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final ClienteService clienteService;
    private final CajaService cajaService;

    public VentaServiceImpl(
            VentaRepository ventaRepository, InventarioService inventarioService,
            CuentaPorCobrarService cuentaPorCobrarService, ClienteService clienteService, CajaService cajaService) {
        this.ventaRepository = ventaRepository;
        this.inventarioService = inventarioService;
        this.cuentaPorCobrarService = cuentaPorCobrarService;
        this.clienteService = clienteService;
        this.cajaService = cajaService;
    }

    /**
     * Deliberadamente SIN {@code @Transactional} propio: cada llamada a
     * {@code ventaRepository} (pre-chequeo, insert, relectura tras colisión) ya
     * corre en su propia transacción vía el manejo transaccional por defecto de
     * Spring Data JPA — un único método siempre transaccional envolviendo las
     * tres reutilizaría la misma sesión de Hibernate para la relectura después
     * de que el insert falle por la restricción única, y una sesión que acaba
     * de lanzar una violación de restricción durante el flush queda en estado
     * no utilizable para seguir operando en la misma transacción. Aislar cada
     * paso en su propia transacción evita ese problema — la única sección que
     * de verdad necesita atomicidad multi-paso es {@code completar()}.
     */
    @Override
    public VentaResumen crear(
            Long tiendaId, Long clienteId, Long vendedorId, List<NuevaLineaVenta> lineas, MetodoPago metodoPago,
            String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        List<LineaVenta> lineasDominio = lineas.stream()
                .map(l -> LineaVenta.nueva(l.productoId(), l.cantidad(), l.precioUnitario()))
                .toList();

        if (correlationIdNormalizado != null) {
            Optional<Venta> existente = ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(
                    tiendaId, vendedorId, correlationIdNormalizado);
            if (existente.isPresent()) {
                return resolverReintentoIdempotente(existente.get(), clienteId, lineasDominio, metodoPago);
            }
        }

        Venta venta = Venta.nueva(clienteId, tiendaId, vendedorId, lineasDominio, metodoPago, correlationIdNormalizado);
        try {
            return toResumen(ventaRepository.save(venta));
        } catch (ReferenciaInvalidaException e) {
            // Dos requests concurrentes con el mismo correlationId: uno gana la
            // inserción, el otro choca contra la restricción única compuesta y
            // el adaptador lo traduce genéricamente a ReferenciaInvalidaException
            // (no distingue esa violación de una FK inválida). Releer antes de
            // decidir: si la fila ya existe, esto era la carrera esperada, no un
            // error real — se resuelve como el mismo reintento idempotente de
            // arriba. Si no existe, sí era una referencia inválida de verdad.
            if (correlationIdNormalizado != null) {
                Optional<Venta> existente = ventaRepository.findByTiendaIdAndVendedorIdAndCorrelationId(
                        tiendaId, vendedorId, correlationIdNormalizado);
                if (existente.isPresent()) {
                    return resolverReintentoIdempotente(existente.get(), clienteId, lineasDominio, metodoPago);
                }
            }
            throw e;
        }
    }

    /** {@code null}/en blanco se tratan igual — sin correlationId, sin idempotencia. */
    private String normalizarCorrelationId(String correlationId) {
        if (correlationId == null) {
            return null;
        }
        String recortado = correlationId.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    /**
     * Un reintento legítimo (mismo cliente/líneas/método) devuelve la venta
     * existente tal cual, sin crear nada — la clave de idempotencia ya garantiza
     * tienda y vendedor correctos (viene de la búsqueda compuesta). Un
     * correlationId reutilizado con contenido distinto es un error del cliente,
     * no un reintento — 409, nunca se devuelven silenciosamente datos de otra
     * venta.
     */
    private VentaResumen resolverReintentoIdempotente(
            Venta existente, Long clienteId, List<LineaVenta> lineas, MetodoPago metodoPago) {
        if (!coincideConLaSolicitud(existente, clienteId, lineas, metodoPago)) {
            throw new CorrelationIdReutilizadoException(existente.getCorrelationId());
        }
        return toResumen(existente);
    }

    private boolean coincideConLaSolicitud(
            Venta existente, Long clienteId, List<LineaVenta> lineas, MetodoPago metodoPago) {
        if (!Objects.equals(existente.getClienteId(), clienteId) || existente.getMetodoPago() != metodoPago) {
            return false;
        }
        List<LineaVenta> lineasExistentes = existente.getLineas();
        if (lineasExistentes.size() != lineas.size()) {
            return false;
        }
        for (int i = 0; i < lineas.size(); i++) {
            LineaVenta esperada = lineasExistentes.get(i);
            LineaVenta recibida = lineas.get(i);
            if (!Objects.equals(esperada.getProductoId(), recibida.getProductoId())
                    || esperada.getCantidad().compareTo(recibida.getCantidad()) != 0
                    || esperada.getPrecioUnitario().compareTo(recibida.getPrecioUnitario()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public VentaResumen completar(Long tiendaId, Long id) {
        return completar(tiendaId, id, List.of());
    }

    @Override
    @Transactional
    public VentaResumen completar(Long tiendaId, Long id, List<PagoInmediato> pagosInmediatos) {
        Venta venta = obtenerORequerida(tiendaId, id);
        List<PagoInmediato> pagos = resolverPagosInmediatos(venta, pagosInmediatos);
        BigDecimal totalInmediato = pagos.stream().map(PagoInmediato::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = venta.total().subtract(totalInmediato);

        if (venta.getMetodoPago() != MetodoPago.CREDITO && !cajaService.hayAbiertaPorTienda(tiendaId)) {
            throw new CajaNoAbiertaException(tiendaId);
        }
        if (venta.getMetodoPago() == MetodoPago.CREDITO || venta.getMetodoPago() == MetodoPago.MIXTO) {
            validarLimiteCredito(tiendaId, venta, saldo);
        }
        venta.completar();
        for (LineaVenta linea : venta.getLineas()) {
            InventarioResumen inventario = inventarioService.obtener(tiendaId, linea.getProductoId());
            inventarioService.registrarMovimiento(
                    tiendaId, linea.getProductoId(), linea.getCantidad(), inventario.costoPromedioActual(),
                    TipoMovimiento.VENTA);
        }
        VentaResumen resumen = toResumen(ventaRepository.save(venta));

        for (PagoInmediato pago : pagos) {
            if (pago.monto().compareTo(BigDecimal.ZERO) > 0) {
                cajaService.registrarMovimientoSiHayAbierta(
                        tiendaId, TipoMovimientoCaja.INGRESO,
                        "Venta #" + resumen.id() + " (" + pago.metodoPago() + ")", pago.monto());
            }
        }
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            cuentaPorCobrarService.crear(resumen.id(), resumen.clienteId(), resumen.tiendaId(), saldo);
        }
        return resumen;
    }

    /**
     * EFECTIVO/TARJETA/TRANSFERENCIA implican un único pago inmediato por el total
     * de la venta — cualquier desglose que mande el cliente para estos métodos se
     * ignora, el servidor es la única fuente de verdad ahí. CREDITO no tiene pago
     * inmediato. Solo MIXTO usa {@code pagosInmediatos} de verdad, validado contra
     * {@code venta.total()}.
     */
    private List<PagoInmediato> resolverPagosInmediatos(Venta venta, List<PagoInmediato> pagosInmediatos) {
        return switch (venta.getMetodoPago()) {
            case EFECTIVO, TARJETA, TRANSFERENCIA -> List.of(new PagoInmediato(venta.getMetodoPago(), venta.total()));
            case CREDITO -> List.of();
            case MIXTO -> validarDesgloseMixto(venta, pagosInmediatos);
        };
    }

    private List<PagoInmediato> validarDesgloseMixto(Venta venta, List<PagoInmediato> pagosInmediatos) {
        if (pagosInmediatos == null || pagosInmediatos.isEmpty()) {
            throw new DesglosePagoInvalidoException(
                    "Una venta MIXTO requiere al menos un pago inmediato — si no hay ninguno, use CREDITO.");
        }
        BigDecimal suma = BigDecimal.ZERO;
        for (PagoInmediato pago : pagosInmediatos) {
            if (pago.metodoPago() == MetodoPago.CREDITO || pago.metodoPago() == MetodoPago.MIXTO) {
                throw new DesglosePagoInvalidoException(
                        "El canal de un pago inmediato no puede ser " + pago.metodoPago() + ".");
            }
            if (pago.monto() == null || pago.monto().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DesglosePagoInvalidoException("Cada pago inmediato debe tener un monto mayor que cero.");
            }
            suma = suma.add(pago.monto());
        }
        if (suma.compareTo(venta.total()) > 0) {
            throw new DesglosePagoInvalidoException(
                    "La suma de los pagos inmediatos (" + suma.toPlainString()
                            + ") no puede exceder el total de la venta (" + venta.total().toPlainString() + ").");
        }
        return pagosInmediatos;
    }

    @Override
    @Transactional
    public VentaResumen anular(Long tiendaId, Long id) {
        Venta venta = obtenerORequerida(tiendaId, id);
        venta.anular();
        return toResumen(ventaRepository.save(venta));
    }

    @Override
    public VentaResumen obtener(Long tiendaId, Long id) {
        return toResumen(obtenerORequerida(tiendaId, id));
    }

    @Override
    public Optional<VentaResumen> buscarPorCorrelationId(Long tiendaId, Long vendedorId, String correlationId) {
        String correlationIdNormalizado = normalizarCorrelationId(correlationId);
        if (correlationIdNormalizado == null) {
            return Optional.empty();
        }
        return ventaRepository
                .findByTiendaIdAndVendedorIdAndCorrelationId(tiendaId, vendedorId, correlationIdNormalizado)
                .map(this::toResumen);
    }

    @Override
    public List<VentaResumen> listarPorTienda(Long tiendaId) {
        return ventaRepository.findByTiendaId(tiendaId).stream().map(this::toResumen).toList();
    }

    @Override
    public Pagina<VentaResumen> listarPorTienda(Long tiendaId, int pagina, int tamano) {
        return ventaRepository.findByTiendaId(tiendaId, pagina, tamano).map(this::toResumen);
    }

    /**
     * Se llama para {@code CREDITO} (saldoFinanciado == total) y para
     * {@code MIXTO} (saldoFinanciado == total menos los pagos inmediatos ya
     * resueltos en {@code resolverPagosInmediatos}, que corren en la misma
     * transacción que esta validación — a diferencia del diseño anterior, ya
     * no hace falta ser conservador validando contra el total completo:
     * cuando esto se ejecuta ya sabemos exactamente cuánto queda pendiente).
     * Alcance por tienda a propósito (no company-wide): esta app siempre
     * opera contra una sola tienda a la vez (ver CLAUDE.md de
     * market-flutter), y las cuentas por cobrar ya están escaneadas por
     * tienda ({@code CuentaPorCobrarService.listarPorTienda}) — sumar la
     * exposición del cliente en otras tiendas requeriría una consulta
     * cross-tienda que no existe hoy. {@code limiteCredito == null} significa
     * sin restricción definida, no Q0.
     *
     * <p>{@code obtenerParaActualizarCredito} (no {@code obtener}) a propósito:
     * bloquea la fila del cliente con {@code PESSIMISTIC_WRITE} hasta que esta
     * transacción termine — sin esto, dos ventas a crédito casi simultáneas del
     * mismo cliente pueden leer el mismo saldo pendiente (ninguna ve la cuenta
     * por cobrar que la otra está a punto de crear) y juntas exceder el límite
     * aunque cada una, evaluada sola, no lo haga. Con el lock, la segunda
     * espera a que la primera termine de commitear y entonces sí ve el saldo
     * ya actualizado.
     */
    private void validarLimiteCredito(Long tiendaId, Venta venta, BigDecimal saldoFinanciado) {
        ClienteResumen cliente = clienteService.obtenerParaActualizarCredito(venta.getClienteId());
        BigDecimal limite = cliente.limiteCredito();
        if (limite == null) {
            return;
        }
        BigDecimal saldoActual = cuentaPorCobrarService.listarPorTienda(tiendaId).stream()
                .filter(c -> c.clienteId().equals(venta.getClienteId()))
                .filter(c -> c.estado() == EstadoCuentaPorCobrar.PENDIENTE)
                .map(CuentaPorCobrarResumen::saldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoProyectado = saldoActual.add(saldoFinanciado);
        if (saldoProyectado.compareTo(limite) > 0) {
            throw new LimiteCreditoExcedidoException(venta.getClienteId(), limite, saldoProyectado);
        }
    }

    private Venta obtenerORequerida(Long tiendaId, Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
        if (!venta.getTiendaId().equals(tiendaId)) {
            throw new ResourceNotFoundException("Venta no encontrada: " + id);
        }
        return venta;
    }

    private VentaResumen toResumen(Venta venta) {
        List<LineaVentaResumen> lineas = venta.getLineas().stream()
                .map(l -> new LineaVentaResumen(l.getId(), l.getProductoId(), l.getCantidad(), l.getPrecioUnitario()))
                .toList();
        return new VentaResumen(
                venta.getId(), venta.getClienteId(), venta.getTiendaId(), venta.getVendedorId(), venta.getFecha(),
                venta.getEstado(), lineas, venta.total(), venta.getMetodoPago());
    }
}
