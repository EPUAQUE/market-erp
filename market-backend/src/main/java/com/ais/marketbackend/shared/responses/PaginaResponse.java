package com.ais.marketbackend.shared.responses;

import com.ais.marketbackend.shared.domain.Pagina;
import java.util.List;
import java.util.function.Function;

public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <S, T> PaginaResponse<T> de(Pagina<S> pagina, Function<S, T> mapper) {
        return new PaginaResponse<>(
                pagina.contenido().stream().map(mapper).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
