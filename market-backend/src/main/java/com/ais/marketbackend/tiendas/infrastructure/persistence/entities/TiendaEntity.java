package com.ais.marketbackend.tiendas.infrastructure.persistence.entities;

import com.ais.marketbackend.tiendas.domain.model.EstadoTienda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "correo", length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTienda estado;

    // Columna plana (no @ManyToOne): grupostienda es dueño de la tabla `grupo_tienda`,
    // mismo patrón que `ProductoTiendaEntity.tiendaId`.
    @Column(name = "grupo_id", nullable = false)
    private Long grupoId;
}
