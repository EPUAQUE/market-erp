package com.ais.marketbackend.traslados.infrastructure.persistence.entities;

import com.ais.marketbackend.traslados.domain.model.EstadoTraslado;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code tiendaOrigenId}/{@code tiendaDestinoId} son columnas planas (no
 * {@code @ManyToOne}): esa tabla es propiedad de otro módulo. {@code lineas} sí
 * usa relación JPA porque es intra-módulo (parte del mismo agregado).
 */
@Entity
@Table(name = "traslado")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrasladoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tienda_origen_id", nullable = false)
    private Long tiendaOrigenId;

    @Column(name = "tienda_destino_id", nullable = false)
    private Long tiendaDestinoId;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTraslado estado;

    @OneToMany(mappedBy = "traslado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LineaTrasladoEntity> lineas = new ArrayList<>();

    public TrasladoEntity(Long id, Long tiendaOrigenId, Long tiendaDestinoId, Instant fecha, EstadoTraslado estado) {
        this.id = id;
        this.tiendaOrigenId = tiendaOrigenId;
        this.tiendaDestinoId = tiendaDestinoId;
        this.fecha = fecha;
        this.estado = estado;
    }
}
