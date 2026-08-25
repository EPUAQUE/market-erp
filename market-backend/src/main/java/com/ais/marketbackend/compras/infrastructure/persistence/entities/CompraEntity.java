package com.ais.marketbackend.compras.infrastructure.persistence.entities;

import com.ais.marketbackend.compras.domain.model.EstadoCompra;
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
 * {@code proveedorId}/{@code tiendaId} son columnas planas (no {@code @ManyToOne}):
 * las tablas {@code proveedor} y {@code tienda} son propiedad de otros módulos.
 * {@code lineas} sí usa relación JPA porque es intra-módulo (parte del mismo
 * agregado).
 */
@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proveedor_id", nullable = false)
    private Long proveedorId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCompra estado;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LineaCompraEntity> lineas = new ArrayList<>();

    public CompraEntity(Long id, Long proveedorId, Long tiendaId, Instant fecha, EstadoCompra estado) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.tiendaId = tiendaId;
        this.fecha = fecha;
        this.estado = estado;
    }
}
