package com.ais.marketbackend.traslados.infrastructure.persistence.entities;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "linea_traslado")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LineaTrasladoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traslado_id", nullable = false)
    private TrasladoEntity traslado;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 0)
    private BigDecimal cantidad;

    public LineaTrasladoEntity(Long id, TrasladoEntity traslado, Long productoId, BigDecimal cantidad) {
        this.id = id;
        this.traslado = traslado;
        this.productoId = productoId;
        this.cantidad = cantidad;
    }
}
