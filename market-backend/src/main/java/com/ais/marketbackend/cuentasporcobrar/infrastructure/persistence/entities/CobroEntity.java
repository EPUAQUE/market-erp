package com.ais.marketbackend.cuentasporcobrar.infrastructure.persistence.entities;

import com.ais.marketbackend.cuentasporcobrar.domain.model.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "cobro")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CobroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_por_cobrar_id", nullable = false)
    private CuentaPorCobrarEntity cuenta;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "monto", nullable = false, precision = 12, scale = 4)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    public CobroEntity(
            Long id, CuentaPorCobrarEntity cuenta, Instant fecha, BigDecimal monto, MetodoPago metodoPago) {
        this.id = id;
        this.cuenta = cuenta;
        this.fecha = fecha;
        this.monto = monto;
        this.metodoPago = metodoPago;
    }
}
