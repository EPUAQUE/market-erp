package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities;

import com.ais.marketbackend.cuentasporcobrar.domain.model.EstadoCuentaPorCobrar;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code ventaId}/{@code clienteId}/{@code tiendaId} son columnas planas (no
 * {@code @ManyToOne}): esas tablas son propiedad de otros módulos. {@code cobros}
 * sí usa relación JPA porque es intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "cuenta_por_cobrar")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CuentaPorCobrarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;

    @Column(name = "monto_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoOriginal;

    @Column(name = "saldo_pendiente", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCuentaPorCobrar estado;

    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CobroEntity> cobros = new ArrayList<>();

    public CuentaPorCobrarEntity(
            Long id, Long ventaId, Long clienteId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
            BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorCobrar estado) {
        this.id = id;
        this.ventaId = ventaId;
        this.clienteId = clienteId;
        this.tiendaId = tiendaId;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.montoOriginal = montoOriginal;
        this.saldoPendiente = saldoPendiente;
        this.estado = estado;
    }
}
