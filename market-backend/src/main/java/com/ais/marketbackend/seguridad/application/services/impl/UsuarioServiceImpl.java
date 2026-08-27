package com.ais.marketbackend.seguridad.application.services.impl;

import com.ais.marketbackend.grupostienda.domain.repository.GrupoTiendaRepository;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioGrupoTiendaResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioResumen;
import com.ais.marketbackend.seguridad.application.dtos.UsuarioTiendaResumen;
import com.ais.marketbackend.seguridad.domain.exception.AsignacionMixtaNoPermitidaException;
import com.ais.marketbackend.seguridad.domain.exception.UsuarioDuplicadoException;
import com.ais.marketbackend.seguridad.domain.model.PermisosEfectivos;
import com.ais.marketbackend.seguridad.domain.model.Rol;
import com.ais.marketbackend.seguridad.domain.model.Usuario;
import com.ais.marketbackend.seguridad.domain.model.UsuarioGrupoTienda;
import com.ais.marketbackend.seguridad.domain.model.UsuarioTienda;
import com.ais.marketbackend.seguridad.domain.repository.RolRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioGrupoTiendaRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioRepository;
import com.ais.marketbackend.seguridad.domain.repository.UsuarioTiendaRepository;
import com.ais.marketbackend.seguridad.domain.service.PermisosEfectivosResolver;
import com.ais.marketbackend.seguridad.domain.service.PoliticaContrasenaValidator;
import com.ais.marketbackend.seguridad.domain.service.SecurityAuditPublisher;
import com.ais.marketbackend.seguridad.domain.service.TipoEventoAuditoria;
import com.ais.marketbackend.seguridad.domain.service.UsernameCanonicalizer;
import com.ais.marketbackend.seguridad.application.services.interfaces.UsuarioService;
import com.ais.marketbackend.seguridad.infrastructure.security.SeguridadProperties;
import com.ais.marketbackend.shared.exceptions.ResourceNotFoundException;
import com.ais.marketbackend.tiendas.domain.model.Tienda;
import com.ais.marketbackend.tiendas.domain.repository.TiendaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioTiendaRepository usuarioTiendaRepository;
    private final UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository;
    private final RolRepository rolRepository;
    private final TiendaRepository tiendaRepository;
    private final GrupoTiendaRepository grupoTiendaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisosEfectivosResolver permisosEfectivosResolver;
    private final SecurityAuditPublisher auditPublisher;
    private final SeguridadProperties properties;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioTiendaRepository usuarioTiendaRepository,
            UsuarioGrupoTiendaRepository usuarioGrupoTiendaRepository,
            RolRepository rolRepository,
            TiendaRepository tiendaRepository,
            GrupoTiendaRepository grupoTiendaRepository,
            PasswordEncoder passwordEncoder,
            PermisosEfectivosResolver permisosEfectivosResolver,
            SecurityAuditPublisher auditPublisher,
            SeguridadProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioTiendaRepository = usuarioTiendaRepository;
        this.usuarioGrupoTiendaRepository = usuarioGrupoTiendaRepository;
        this.rolRepository = rolRepository;
        this.tiendaRepository = tiendaRepository;
        this.grupoTiendaRepository = grupoTiendaRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisosEfectivosResolver = permisosEfectivosResolver;
        this.auditPublisher = auditPublisher;
        this.properties = properties;
    }

    @Override
    @Transactional
    public UsuarioResumen crear(
            String username, String passwordPlano, String nombre, String telefono, String correo) {
        String usernameCanonico = UsernameCanonicalizer.canonicalizar(username);
        PoliticaContrasenaValidator.validar(
                passwordPlano, properties.passwordPolicy().minLength(), properties.passwordPolicy().maxLength());

        if (usuarioRepository.existsByUsername(usernameCanonico)) {
            throw new UsuarioDuplicadoException(usernameCanonico);
        }

        Usuario usuario = Usuario.nuevo(
                usernameCanonico, passwordEncoder.encode(passwordPlano), nombre, telefono, correo);
        Usuario guardado = usuarioRepository.save(usuario);
        auditPublisher.publicar(
                TipoEventoAuditoria.USUARIO_CREADO, UUID.randomUUID().toString(), "usuarioId=" + guardado.getId());
        return toResumen(guardado);
    }

    @Override
    @Transactional
    public void asignarTienda(Long usuarioId, Long tiendaId, Long rolId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + rolId));
        Tienda tienda = tiendaRepository.findById(tiendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + tiendaId));

        boolean yaTieneElGrupoDeEstaTienda = usuarioGrupoTiendaRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(ug -> ug.getGrupoTiendaId().equals(tienda.getGrupoId()));
        if (yaTieneElGrupoDeEstaTienda) {
            throw new AsignacionMixtaNoPermitidaException(
                    "El usuario ya tiene asignado el grupo de tiendas al que pertenece la tienda " + tiendaId);
        }

        usuarioTiendaRepository.save(new UsuarioTienda(null, usuarioId, tiendaId, rol));
        auditPublisher.publicar(TipoEventoAuditoria.TIENDA_ASIGNADA, UUID.randomUUID().toString(),
                "usuarioId=" + usuarioId + ",tiendaId=" + tiendaId + ",rolId=" + rolId);
    }

    @Override
    public List<UsuarioTiendaResumen> listarTiendas(Long usuarioId) {
        return usuarioTiendaRepository.findByUsuarioId(usuarioId).stream()
                .map(ut -> new UsuarioTiendaResumen(ut.getId(), ut.getTiendaId(), ut.getRol().getId(), ut.getRol().getNombre()))
                .toList();
    }

    @Override
    @Transactional
    public void asignarGrupo(Long usuarioId, Long grupoTiendaId, Long rolId) {
        usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + rolId));
        grupoTiendaRepository.findById(grupoTiendaId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de tiendas no encontrado: " + grupoTiendaId));

        List<Long> tiendaIdsDelGrupo = tiendaRepository.listarIdsPorGrupo(grupoTiendaId);
        boolean yaTieneUnaTiendaDeEsteGrupo = usuarioTiendaRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(ut -> tiendaIdsDelGrupo.contains(ut.getTiendaId()));
        if (yaTieneUnaTiendaDeEsteGrupo) {
            throw new AsignacionMixtaNoPermitidaException(
                    "El usuario ya tiene asignada una tienda individual del grupo " + grupoTiendaId);
        }

        usuarioGrupoTiendaRepository.save(new UsuarioGrupoTienda(null, usuarioId, grupoTiendaId, rol));
        auditPublisher.publicar(TipoEventoAuditoria.GRUPO_ASIGNADO, UUID.randomUUID().toString(),
                "usuarioId=" + usuarioId + ",grupoTiendaId=" + grupoTiendaId + ",rolId=" + rolId);
    }

    @Override
    public List<UsuarioGrupoTiendaResumen> listarGrupos(Long usuarioId) {
        return usuarioGrupoTiendaRepository.findByUsuarioId(usuarioId).stream()
                .map(ug -> new UsuarioGrupoTiendaResumen(
                        ug.getId(), ug.getGrupoTiendaId(), ug.getRol().getId(), ug.getRol().getNombre()))
                .toList();
    }

    @Override
    public UsuarioResumen obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .map(this::toResumen)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    @Override
    public PermisosEfectivos obtenerPermisosEfectivos(Long usuarioId) {
        return permisosEfectivosResolver.resolver(usuarioId);
    }

    @Override
    public PermisosEfectivos obtenerPermisosEfectivosPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
        return permisosEfectivosResolver.resolver(usuario.getId());
    }

    @Override
    public List<UsuarioResumen> listar() {
        return usuarioRepository.findAll().stream().map(this::toResumen).toList();
    }

    private UsuarioResumen toResumen(Usuario usuario) {
        return new UsuarioResumen(
                usuario.getId(), usuario.getUsername(), usuario.getEstado(), usuario.getNombre(),
                usuario.getTelefono(), usuario.getCorreo());
    }
}
