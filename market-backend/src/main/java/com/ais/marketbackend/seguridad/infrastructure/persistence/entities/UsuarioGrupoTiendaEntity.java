package com.ais.marketbackend.seguridad.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@code grupo_tienda_id} es un identificador plano (sin {@code @ManyToOne} a una
 * entidad GrupoTienda: ese agregado es propiedad del módulo grupostienda) — mismo
 * patrón que {@code UsuarioTiendaEntity.tiendaId}.
 */
@Entity
@Table(name = "usuario_grupo_tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UsuarioGrupoTiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "grupo_tienda_id", nullable = false)
    private Long grupoTiendaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolEntity rol;
}
