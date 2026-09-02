package com.ais.marketbackend.cuentasporpagar.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private CuentaPorPagarEntity cuenta;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    public PagoEntity(Long id, CuentaPorPagarEntity cuenta, Instant fecha, BigDecimal monto) {
        this.id = id;
        this.cuenta = cuenta;
        this.fecha = fecha;
        this.monto = monto;
    }
}
