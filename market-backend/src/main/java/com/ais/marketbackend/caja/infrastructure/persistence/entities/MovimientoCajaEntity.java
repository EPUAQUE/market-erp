package com.ais.marketbackend.caja.infrastructure.persistence.entities;

import com.ais.marketbackend.caja.domain.model.TipoMovimientoCaja;
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
@Table(name = "movimiento_caja")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoCajaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_sesion_id", nullable = false)
    private CajaSesionEntity sesion;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoMovimientoCaja tipo;

    @Column(name = "concepto", nullable = false, length = 255)
    private String concepto;

    @Column(name = "monto", nullable = false, precision = 12, scale = 4)
    private BigDecimal monto;

    public MovimientoCajaEntity(
            Long id, CajaSesionEntity sesion, Instant fecha, TipoMovimientoCaja tipo, String concepto, BigDecimal monto) {
        this.id = id;
        this.sesion = sesion;
        this.fecha = fecha;
        this.tipo = tipo;
        this.concepto = concepto;
        this.monto = monto;
    }
}
