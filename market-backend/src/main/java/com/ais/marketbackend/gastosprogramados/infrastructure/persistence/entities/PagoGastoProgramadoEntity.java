package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities;

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
@Table(name = "pago_gasto_programado")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PagoGastoProgramadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gasto_programado_id", nullable = false)
    private GastoProgramadoEntity gasto;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "monto", nullable = false, precision = 12, scale = 4)
    private BigDecimal monto;

    public PagoGastoProgramadoEntity(Long id, GastoProgramadoEntity gasto, Instant fecha, BigDecimal monto) {
        this.id = id;
        this.gasto = gasto;
        this.fecha = fecha;
        this.monto = monto;
    }
}
