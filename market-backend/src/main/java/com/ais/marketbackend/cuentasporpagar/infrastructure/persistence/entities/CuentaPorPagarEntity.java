package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities;

import com.ais.marketbackend.cuentasporpagar.domain.model.EstadoCuentaPorPagar;
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
 * {@code compraId}/{@code proveedorId}/{@code tiendaId} son columnas planas (no
 * {@code @ManyToOne}): esas tablas son propiedad de otros módulos. {@code pagos}
 * sí usa relación JPA porque es intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "cuenta_por_pagar")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CuentaPorPagarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compra_id", nullable = false)
    private Long compraId;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;

    @Column(name = "monto_original", nullable = false, precision = 12, scale = 4)
    private BigDecimal montoOriginal;

    @Column(name = "saldo_pendiente", nullable = false, precision = 12, scale = 4)
    private BigDecimal saldoPendiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCuentaPorPagar estado;

    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PagoEntity> pagos = new ArrayList<>();

    public CuentaPorPagarEntity(
            Long id, Long compraId, Long proveedorId, Long tiendaId, Instant fechaEmision, Instant fechaVencimiento,
            BigDecimal montoOriginal, BigDecimal saldoPendiente, EstadoCuentaPorPagar estado) {
        this.id = id;
        this.compraId = compraId;
        this.proveedorId = proveedorId;
        this.tiendaId = tiendaId;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.montoOriginal = montoOriginal;
        this.saldoPendiente = saldoPendiente;
        this.estado = estado;
    }
}
