package com.ais.marketbackend.seguridad.infrastructure.security;

import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioGrupoTiendaRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Unión de permisos de todos los roles asignados a un usuario (globales o por
 * tienda). Cachea el resultado con TTL corto — la base de datos sigue siendo la
 * fuente de verdad. {@code UsuarioServiceImpl} llama a {@link #invalidar} tras
 * cada cambio que puede afectar los permisos de un usuario (asignar tienda/grupo,
 * desactivar/bloquear/activar), así que en la práctica el efecto es inmediato en
 * esta instancia; sin esa invalidación explícita (o en una instancia distinta a la
 * que procesó el cambio, ver PLAN_MEJORAS.md Fase 11 — multi-instancia) el peor
 * caso sigue siendo {@link #CACHE_TTL_SEGUNDOS} segundos.
 */
@Component
public class PermisosEfectivosResolverImpl implements PermisosEfectivosResolver {

    private static final long CACHE_TTL_SEGUNDOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final UsuarioTiendaRepository usuarioTiendaRepository;
    private final UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository;
    private final TiendaRepository tiendaRepository;
    private final ConcurrentHashMap<Long, Entrada> cache = new ConcurrentHashMap<>();

    public PermisosEfectivosResolverImpl(
            UsuarioRepository usuarioRepository, UsuarioTiendaRepository usuarioTiendaRepository,
            UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository, TiendaRepository tiendaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioTiendaRepository = usuarioTiendaRepository;
        this.usuarioGrupoTiendaRepository = usuarioGrupoTiendaRepository;
        this.tiendaRepository = tiendaRepository;
    }

    @Override
    public PermisosEfectivos resolver(Long usuarioId) {
        Entrada entrada = cache.get(usuarioId);
        Instant ahora = Instant.now();
        if (entrada != null && entrada.expiraEn.isAfter(ahora)) {
            return entrada.valor;
        }
        PermisosEfectivos calculado = calcular(usuarioId);
        cache.put(usuarioId, new Entrada(calculado, ahora.plusSeconds(CACHE_TTL_SEGUNDOS)));
        return calculado;
    }

    @Override
    public void invalidar(Long usuarioId) {
        cache.remove(usuarioId);
    }

    private PermisosEfectivos calcular(Long usuarioId) {
        String username = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId))
                .getUsername();

        List<UsuarioTienda> asignaciones = usuarioTiendaRepository.findByUsuarioId(usuarioId);
        List<UsuarioGrupoTienda> asignacionesGrupo = usuarioGrupoTiendaRepository.findByUsuarioId(usuarioId);

        Set<String> permisos = new HashSet<>();
        Set<Long> tiendaIds = new HashSet<>();
        Set<Long> grupoIds = new HashSet<>();
        boolean alcanceGlobal = false;

        for (UsuarioTienda asignacion : asignaciones) {
            Rol rol = asignacion.getRol();
            rol.getPermisos().forEach(p -> permisos.add(p.getCodigo()));
            tiendaIds.add(asignacion.getTiendaId());
            if (rol.isAlcanceGlobal()) {
                alcanceGlobal = true;
            }
        }

        for (UsuarioGrupoTienda asignacion : asignacionesGrupo) {
            Rol rol = asignacion.getRol();
            rol.getPermisos().forEach(p -> permisos.add(p.getCodigo()));
            grupoIds.add(asignacion.getGrupoTiendaId());
            tiendaIds.addAll(tiendaRepository.listarIdsPorGrupo(asignacion.getGrupoTiendaId()));
            if (rol.isAlcanceGlobal()) {
                alcanceGlobal = true;
            }
        }

        return new PermisosEfectivos(
                usuarioId, username, Set.copyOf(permisos), Set.copyOf(tiendaIds), alcanceGlobal,
                Set.copyOf(grupoIds));
    }

    private record Entrada(PermisosEfectivos valor, Instant expiraEn) {
    }
}
