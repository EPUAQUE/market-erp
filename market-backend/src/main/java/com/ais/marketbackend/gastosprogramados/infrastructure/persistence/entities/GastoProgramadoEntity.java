package com.ais.marketbackend.gastosprogramados.infrastructure.persistence.entities;

import com.ais.marketbackend.gastosprogramados.domain.model.FrecuenciaGasto;
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
 * {@code tiendaId} es columna plana (no {@code @ManyToOne}): esa tabla es
 * propiedad de otro módulo. {@code pagos} sí usa relación JPA porque es
 * intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "gasto_programado")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GastoProgramadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "concepto", nullable = false, length = 150)
    private String concepto;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia", nullable = false, length = 20)
    private FrecuenciaGasto frecuencia;

    @Column(name = "proxima_fecha", nullable = false)
    private Instant proximaFecha;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @OneToMany(mappedBy = "gasto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PagoGastoProgramadoEntity> pagos = new ArrayList<>();

    public GastoProgramadoEntity(
            Long id, Long tiendaId, String concepto, BigDecimal monto, FrecuenciaGasto frecuencia,
            Instant proximaFecha, boolean activo) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.concepto = concepto;
        this.monto = monto;
        this.frecuencia = frecuencia;
        this.proximaFecha = proximaFecha;
        this.activo = activo;
    }
}
