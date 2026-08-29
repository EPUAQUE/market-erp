package com.ais.marketbackend.seguridad.domain.repository;

import com.ais.marketbackend.seguridad.domain.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Usuario save(Usuario usuario);

    Optional<Usuario> findById(Long id);

    /**
     * Igual que {@link #findById}, pero bloquea la fila con
     * {@code PESSIMISTIC_WRITE} dentro de la transacción actual — usado por
     * {@code UsuarioServiceImpl.asignarTienda}/{@code asignarGrupo} para
     * serializar la validación "asignación mixta no permitida" entre sí, aunque
     * escriban en tablas distintas ({@code usuario_tienda} vs
     * {@code usuario_grupo_tienda}, sin restricción de BD que abarque ambas).
     * Sin esto, un {@code asignarTienda} y un {@code asignarGrupo} concurrentes
     * para el mismo usuario podían leer cada uno la otra tabla vacía y ambos
     * pasar la validación, dejando al usuario con una tienda individual y el
     * grupo de esa misma tienda a la vez.
     */
    Optional<Usuario> findByIdConBloqueo(Long id);

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findAll();
}
