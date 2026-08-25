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
 * {@code tienda_id} es un identificador plano a nivel de Java (sin {@code @ManyToOne}
 * a una entidad Tienda: ese agregado es propiedad del módulo Tiendas y Seguridad no
 * debe depender de su infraestructura). La restricción FK en base de datos sí existe
 * — se agrega en {@code tiendas/001-tienda.xml}, una vez que ese módulo publica su
 * changelog.
 */
@Entity
@Table(name = "usuario_tienda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UsuarioTiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private RolEntity rol;
}
