package com.ais.marketbackend.shared.infrastructure.persistence;

import com.ais.marketbackend.shared.domain.Pagina;
import org.springframework.data.domain.Page;

/** Único punto de conversión entre {@code Page} de Spring Data y {@link Pagina} de dominio. */
public final class PaginaMapper {

    private PaginaMapper() {
    }

    public static <T> Pagina<T> desde(Page<T> page) {
        return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
