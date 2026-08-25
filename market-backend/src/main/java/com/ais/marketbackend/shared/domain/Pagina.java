package com.ais.marketbackend.shared.domain;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado, framework-agnóstico — el dominio y la capa de aplicación no
 * conocen {@code org.springframework.data.domain.Page}; la conversión desde/hacia
 * Spring Data vive en {@code shared.infrastructure.persistence.PaginaMapper}
 * (adaptadores) y {@code shared.responses.PaginaResponse} (controllers).
 */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public <R> Pagina<R> map(Function<T, R> mapper) {
        return new Pagina<>(contenido.stream().map(mapper).toList(), pagina, tamano, totalElementos, totalPaginas);
    }
}
