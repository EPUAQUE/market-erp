package com.ais.marketbackend.gastosprogramados.domain.model;

public enum FrecuenciaGasto {
    SEMANAL(7),
    QUINCENAL(15),
    MENSUAL(30),
    ANUAL(365);

    private final int dias;

    FrecuenciaGasto(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }
}
