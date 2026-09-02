package com.ais.marketbackend.caja.infrastructure.persistence.entities;

import com.ais.marketbackend.caja.domain.model.EstadoCajaSesion;
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
 * {@code tiendaId} es una columna plana (no {@code @ManyToOne}): esa tabla es
 * propiedad de otro módulo. {@code movimientos} sí usa relación JPA porque es
 * intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "caja_sesion")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CajaSesionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_final_contado", precision = 12, scale = 2)
    private BigDecimal montoFinalContado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCajaSesion estado;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MovimientoCajaEntity> movimientos = new ArrayList<>();

    @Column(name = "correlation_id_apertura", length = 100)
    private String correlationIdApertura;

    @Column(name = "correlation_id_cierre", length = 100)
    private String correlationIdCierre;

    public CajaSesionEntity(
            Long id, Long tiendaId, Instant fechaApertura, Instant fechaCierre, BigDecimal montoInicial,
            BigDecimal montoFinalContado, EstadoCajaSesion estado, String correlationIdApertura,
            String correlationIdCierre) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.montoInicial = montoInicial;
        this.montoFinalContado = montoFinalContado;
        this.estado = estado;
        this.correlationIdApertura = correlationIdApertura;
        this.correlationIdCierre = correlationIdCierre;
    }
}
