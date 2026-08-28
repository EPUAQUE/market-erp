package com.ais.marketbackend.fel.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador atómico del correlativo fiscal por (tienda, serie), separado de
 * {@link DocumentoFelEntity}: reservar el siguiente número es una operación propia,
 * bloqueada con {@code PESSIMISTIC_WRITE}, independiente de si el documento que lo usa
 * llega a guardarse — evita duplicados bajo emisión concurrente a costa de permitir
 * huecos en la numeración, aceptable fiscalmente; los duplicados no lo son.
 */
@Entity
@Table(name = "fel_correlativo")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FelCorrelativoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    @Column(name = "serie", nullable = false, length = 10)
    private String serie;

    @Column(name = "siguiente_numero", nullable = false)
    private long siguienteNumero;
}
