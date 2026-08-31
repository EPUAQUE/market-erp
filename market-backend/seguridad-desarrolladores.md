# Seguridad para desarrolladores

Guía operativa del módulo de autenticación y autorización de `market-backend`
(`com.ais.marketbackend.seguridad`).
Stack: Java 25, Spring Boot 4, Spring Security, JWT (RS256) + refresh token opaco,
Argon2id, PostgreSQL + Liquibase.

> Este es un **login propietario** que emite access tokens JWT de corta duración más un
> refresh token opaco para renovarlos. **No** es un flujo OAuth 2.0 / OpenID Connect
> completo. Si se necesita interoperar con un IdP externo, usar un proveedor de identidad
> aparte de este módulo.

---

## 1. Modelo de autorización

RBAC plano con alcance por tienda (multi-tienda es un requisito de negocio central, no un
detalle técnico):

1. **Usuario** — identidad y estado de la cuenta.
2. **Rol** (`ADMIN`, `ENCARGADO_TIENDA`, `CAJERO`, `AUDITOR`, …) — catálogo de roles de
   negocio.
3. **Permiso** — código plano `{MODULO}_{ACCION}` (ej. `PRODUCTOS_VER`, `VENTAS_CREAR`,
   `CAJA_CERRAR`).
4. **RolPermiso** — asignación many-to-many; el conjunto efectivo de permisos de un
   usuario es la **unión** de los permisos de todos sus roles.
5. **UsuarioTienda** — asignación de un usuario a una o varias tiendas, cada una con un
   rol. Determina el **alcance** sobre el que puede operar, no solo qué puede hacer.

Regla de permiso:

```text
permitido = el usuario tiene, en alguno de sus roles (globales o de la tienda
            solicitada), el permiso requerido por el endpoint,
            Y la tienda solicitada está entre sus tiendas asignadas
            (o su rol es explícitamente multi-tienda, ej. ADMIN).
```

- **Fail-closed**: cualquier ausencia, duplicado, corrupción o ambigüedad de datos de
  permisos produce **denegación** y un evento de auditoría.
- Un permiso concedido **no** implica acceso a cualquier tienda: siempre se valida el
  alcance de `UsuarioTienda` para operaciones sobre recursos tienda-específicos
  (inventario, caja, ventas, traslados).

---

## 2. Cómo proteger un nuevo `RestController`

1. Definir (o reutilizar) un código de permiso plano en el catálogo `Permiso`
   (`MODULO_ACCION`, MAYÚSCULAS_SNAKE).
2. Anotar el método con `@RequiresPermission("CODIGO_PERMISO")`.

```java
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    @GetMapping("/{id}")
    @RequiresPermission("PRODUCTOS_VER")
    public ProductoResponse get(@PathVariable Long id) { ... }

    @PostMapping
    @RequiresPermission("PRODUCTOS_CREAR")
    public ProductoResponse create(@RequestBody @Valid CrearProductoRequest req) { ... }
}
```

- Al arrancar, un validador de catálogo verifica que **todos** los códigos referenciados
  existan en la tabla `permiso`. Si falta alguno, **la aplicación no arranca**.
- Endpoints sin `@RequiresPermission` quedan igualmente **autenticados** por defecto
  (fail-closed en la capa HTTP), pero **no** tienen autorización de negocio; todo endpoint
  que mute o exponga datos de negocio debe anotarse.

## 3. Alcance por tienda (equivalente a BOLA/IDOR)

`@RequiresPermission` autoriza la **acción**, no el **alcance**. Para recursos
tienda-específicos, valida siempre que la tienda del recurso esté entre las tiendas
asignadas al usuario (o que su rol sea multi-tienda):

```java
@Transactional(readOnly = true)
public InventarioResponse get(Long tiendaId, Long productoId, Jwt principal) {
    if (!tiendaScopeResolver.tieneAcceso(principal, tiendaId)) {
        throw new AccessDeniedException("tienda_fuera_de_alcance");
    }
    Inventario inventario = inventarioRepository.findByTiendaIdAndProductoId(tiendaId, productoId)
            .orElseThrow(ResourceNotFoundException::new);
    return map(inventario);
}
```

Devolver `403` genérico sin revelar si el recurso existe en una tienda ajena.

---

## 4. Autenticación y contraseñas (Argon2id)

- Endpoint único de login: `POST /api/v1/auth/login` con `{ "username", "password" }`.
- **No existe** `/auth/salt` ni hashing en cliente. Argon2id genera y gestiona su propio
  sal interno.
- La contraseña se verifica con `PasswordEncoder.matches()`. **Nunca** se registra,
  retorna ni propaga en claro.
- `matches()` se ejecuta **siempre**, incluso si el usuario no existe (credencial
  ficticia), para no filtrar por tiempo.
- Toda falla (usuario inexistente, contraseña incorrecta, cuenta inactiva) produce la
  **misma** respuesta `401 AUTHENTICATION_FAILED`.

### Parámetros Argon2id

Definidos en `PasswordEncoderConfig`: `m=19456 KiB`, `t=2`, `p=1`, sal 16 bytes, hash 32
bytes (mínimo de referencia OWASP vigente). **Acción requerida por entorno**: realizar una
prueba de rendimiento en el hardware de producción y **subir el costo** hasta que una
verificación tarde ~0.25–0.5 s sin causar DoS. Documentar los valores finales.

### Política de contraseña

`PasswordPolicy`: longitud mínima configurable (≥12), máxima (≥64), medida en **code
points**. Se permiten todos los caracteres Unicode y espacios; **sin** reglas de
composición; **sin** truncado ni normalización.

### Cambio de contraseña y restablecimiento administrativo

- **Autoservicio**: `POST /api/v1/auth/password` (`{ "passwordActual", "passwordNueva" }`,
  usuario autenticado, sin permiso adicional). Verifica la actual con `matches()`, valida
  la nueva contra `PasswordPolicy`, y **revoca todos los refresh tokens del usuario**
  (`RefreshTokenRepository.revocarTodosDeUsuario`) — cualquier otra sesión activa debe
  volver a iniciar sesión. `Usuario.cambiarPassword` sube `version_seguridad`.
- **Restablecimiento administrativo**: `POST /api/v1/usuarios/{usuarioId}/password/restablecer`
  (requiere `USUARIOS_RESTABLECER_PASSWORD`, solo ADMIN). El backend genera una
  contraseña temporal aleatoria (`TemporaryPasswordGenerator`, 20 caracteres, charset sin
  ambiguos) y la devuelve **una sola vez** en la respuesta — nunca se persiste en claro ni
  se registra en auditoría. Marca `usuario.debe_cambiar_password = true`
  (`Usuario.restablecerConPasswordTemporal`) y también revoca todos sus refresh tokens.
- **`debe_cambiar_password`**: el login (`/auth/login`, `/auth/refresh`) incluye
  `debeCambiarPassword` en la respuesta cuando está marcado — el frontend debe forzar la
  pantalla de cambio antes de permitir cualquier otra acción. Cambiar la contraseña (por
  cualquiera de los dos caminos de arriba) limpia la marca. El propio access token lleva
  el claim `debeCambiarPassword`, y `DebeCambiarPasswordFilter` (ver §5) bloquea con `403
  DEBE_CAMBIAR_PASSWORD` cualquier ruta que no sea `/api/v1/auth/password`, `/logout` o
  `/me` mientras esté activo — ya no es solo una señal para el frontend.

---

## 5. Access token y refresh token

- Firma **asimétrica RS256** para el access token. Componentes estándar: `JwtEncoder`
  (emisión), `JwtDecoder` (validación), sin filtro criptográfico propio.
- El algoritmo se **fija** en la configuración. **Nunca** se confía en el encabezado
  `alg`. Se rechaza `alg=none`, algoritmos inesperados, `kid` desconocido y tokens
  malformados.
- Claims del access token: `sub` (código público del usuario), `iss`, `aud`, `iat`,
  `nbf`, `exp`, `jti`, `tiendas` (tiendas activas del usuario, cuando aplique), `sver`
  (versión de seguridad del usuario al momento de emitir el token) y
  `debeCambiarPassword`. Tolerancia de reloj configurable (`clock-skew`, por defecto
  30s). TTL corto (por defecto 10 min).
- El JWT **no cifra** su payload: **nunca** incluir contraseñas, hashes, sales, correos,
  nombres ni IP.

### Refresh token

- **Opaco** (no JWT), aleatorio, **rotatorio** y de **un solo uso**.
- Se almacena en servidor **hasheado**, nunca en claro, junto a `usuario_id`,
  `expira_en`, `revocado` y `token_padre_id`.
- `POST /api/v1/auth/refresh`: valida hash + vigencia + no revocado, emite access token
  nuevo y refresh token nuevo, y revoca el usado.
- **Reutilización de un refresh ya usado/revocado** ⇒ se revoca toda la cadena del
  usuario y se audita como posible robo de token.
- `POST /api/v1/auth/logout` revoca el refresh activo. El access token no se invalida
  (stateless): expira solo por TTL corto.

### Rotación de llaves (access token)

1. Generar el nuevo par fuera del código (ver §10) con un `kid` nuevo.
2. Añadir su llave **pública** como `app.security.jwt.keys[n]` (coexistencia): los
   tokens antiguos siguen validando.
3. Cambiar `app.security.jwt.active-kid` al nuevo `kid` y montar su llave privada.
4. Tras `exp` del último token firmado con la llave anterior, retirar la vieja.

### Invalidación temprana (antes de `exp`) y revocar sesiones

`Usuario.versionSeguridad` (incrementada por `cambiarPassword`/
`restablecerConPasswordTemporal`/`desactivar`/`bloquear`/`activar`/`revocarSesiones`)
viaja en el access token como claim `sver` y se revalida en **cada petición
autenticada** vía `SecurityVersionValidator` (`OAuth2TokenValidator<Jwt>`, registrado
en `JwtConfig.defaultValidators`) — no solo en los endpoints anotados con
`@RequiresPermission` (a diferencia de `PermissionInterceptor`), así que cierra el
hueco para cualquier ruta autenticada. Si la versión no coincide, el usuario no existe
o no está activo, el token se rechaza con `401 AUTHENTICATION_FAILED` aunque siga
vigente su `exp` — cambiar contraseña, bloquear/desactivar la cuenta o revocar
sesiones invalida de inmediato cualquier access token ya emitido, sin esperar el TTL.
`POST /api/v1/usuarios/{usuarioId}/sesiones/revocar` (`USUARIOS_REVOCAR_SESIONES`,
solo ADMIN) expone esto para revocar la sesión de otro usuario sin tocar su
contraseña ni su estado — sube `versionSeguridad` y revoca todos sus refresh tokens
(`UsuarioServiceImpl.revocarSesiones`).

---

## 6. Rate limiting y anti-automatización

- Límites por **IP de origen validada** y por **HMAC del username canónico**.
  Configurables en `app.security.rate-limit.*`.
- Respuesta `429` con `Retry-After`, sin revelar si el usuario existe.
- El token bucket **se recarga** de forma continua: no hay bloqueos permanentes por
  username.
- `X-Forwarded-For` **solo** se respeta si la conexión viene de un proxy en
  `app.security.network.trusted-proxies`.
- La implementación por defecto es **en memoria (una instancia)**. En multi-instancia
  debe registrarse otro bean `LoginRateLimiter` con almacén compartido (p. ej. Redis).
- Los buckets en memoria (uno por IP y uno por hash de username) se purgan
  periódicamente (`app.security.rate-limit.login.cleanup-interval`, por defecto 10
  minutos) una vez que se recargan por completo — sin esto, el mapa crecía sin
  límite con cada IP/usuario distinto visto.

---

## 7. Errores, auditoría y logging

- Formato JSON uniforme (`ApiErrorResponse`): `timestamp`, `status`, `error`, `message`
  genérico, `path`, `correlationId`. **Nunca** stack traces, SQL, nombres de tabla ni
  claims internos.
- Códigos: `400` validación/JSON, `401` no autenticado/token inválido, `403` permiso o
  tienda insuficiente, `429` rate limit.
- Auditoría: login OK/fallido, rate limit, token inválido, reutilización de refresh,
  permiso/tienda denegados, datos de permiso corruptos, cambio de rol/tienda/estado de
  cuenta, `kid` desconocido. Con correlation ID y datos mínimos.
- **Prohibido registrar**: contraseñas, hashes, bearer tokens, refresh tokens (ni su
  hash), claves privadas o datos personales completos. Las entradas se **sanitizan**.

---

## 8. TLS, cookies y frontend

- **TLS/HTTPS es obligatorio en producción**. **HSTS** debe habilitarse en el
  borde/proxy correspondiente.
- El access token viaja en `Authorization: Bearer`; no se guarda en cookies.
- Si el refresh token se entrega en cookie, debe ser `HttpOnly`, `Secure`,
  `SameSite=Strict`, y CSRF **se reactiva** para el endpoint de refresh/logout.
- El backoffice (Vue) mantiene el access token **solo en memoria** (no
  `localStorage`/`sessionStorage`). El POS (Flutter) documenta por separado su
  estrategia de almacenamiento seguro del refresh token (ej. secure storage del SO).

---

## 9. Gestión de llaves (sin secretos en el repo)

Generar el par RSA fuera del código y montarlo de forma segura (gestor de secretos,
keystore o archivos montados). Ejemplo local (no usar estas llaves en producción):

```bash
# Llave privada PKCS#8 (PEM "PRIVATE KEY")
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private-key-2026-01.pem
# Llave pública X.509 (PEM "PUBLIC KEY")
openssl rsa -in private-key-2026-01.pem -pubout -out public-key-2026-01.pem
```

Luego apuntar `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` a esas rutas
(`file:...`). Las llaves y el `.env` están en `.gitignore`. **Nunca** versionar llaves
privadas, `.env` ni credenciales reales.

---

## 10. Pruebas de seguridad

Ejecutar todo el conjunto:

```bash
./mvnw test
```

Cobertura incluida:

- **Autenticación**: éxito, usuario inexistente vs contraseña incorrecta (misma
  respuesta), cuenta inactiva, Argon2 ejecutado también sin usuario, rate limit previo
  al acceso a BD.
- **Refresh token**: rotación válida, reutilización revoca cadena, expiración, logout.
- **JWT**: firma válida/inválida, expirado (dentro/fuera de skew), aún no válido,
  `iss`/`aud` incorrectos, `alg=none`, `kid` desconocido, claim obligatoria ausente.
- **Autorización**: permiso concedido/denegado, tienda fuera de alcance, rol
  multi-tienda, unión de múltiples roles, `sver` desfasado, catálogo de permisos
  corrupto.
- **HTTP**: solo login/refresh públicos, `401` sin token, respuestas no cacheables,
  CORS por allowlist, encabezados de seguridad.

**Al crear un nuevo endpoint**, agregar como mínimo: un test de acceso concedido, uno de
acceso denegado por permiso, uno de acceso sin token (`401`), y —si opera sobre un
recurso tienda-específico— un test de alcance de tienda.

---

## 11. Responsabilidades de infraestructura

Fuera del código de la aplicación, deben resolverse en la plataforma:

- **TLS/HTTPS** y terminación segura; **HSTS** en el borde.
- **Proxy confiable** y `trusted-proxies` correctos para `X-Forwarded-For`.
- **Gestor de secretos** para llaves JWT y credenciales de BD.
- **Rate limiting distribuido** (almacén compartido) en multi-instancia.
- **Límites de tamaño de headers/cuerpo** en el proxy.
- **Sincronización de reloj** (NTP) coherente con el `clock-skew` configurado.

---

## 12. Migraciones (Liquibase, PostgreSQL)

- `001-usuario.xml` — tabla `usuario` con `password_hash`, `estado`, `version_seg`,
  unicidad de `username`.
- `002-rol-permiso.xml` — tablas `rol`, `permiso`, `rol_permiso`.
- `003-usuario-tienda.xml` — tabla `usuario_tienda` (usuario, tienda, rol).
- `004-refresh-token.xml` — tabla `refresh_token` (hash, `usuario_id`, `expira_en`,
  `revocado`, `token_padre_id`).

Un changeset por cambio de esquema; nunca editar un changeset ya aplicado en un ambiente
compartido — se agrega uno nuevo.

---

## 13. Política de contraseña, bloqueo, recuperación y baja de empleados

Fase 4 (PLAN_MEJORAS.md) pedía definir esto explícitamente — acá se documenta lo
que YA aplica el código (no se inventó nada nuevo salvo lo marcado "Fase 4, nuevo"),
más las decisiones de política que no eran puramente técnicas.

### Contraseña

- Longitud: 12–64 code points (`PasswordPolicy`, §4), sin reglas de composición,
  Unicode completo permitido. **Decisión**: no se agregan reglas de composición
  (mayúscula+número+símbolo, etc.) — la evidencia de NIST/OWASP actual las
  desaconseja (empujan a patrones predecibles); longitud mínima + Argon2id ya dan
  más resistencia real a fuerza bruta.
- Expiración forzada por tiempo: **no implementada, decisión explícita de no
  hacerlo** — expirar contraseñas periódicamente sin motivo también está
  desaconsejado por la guía NIST vigente (empuja a variaciones triviales
  predecibles); el mecanismo real de forzar un cambio ya existe y se usa donde
  corresponde (`debe_cambiar_password`, ver §4) — al restablecer administrativamente,
  no en un cronograma arbitrario.

### Bloqueo

Dos mecanismos, en capas distintas:

- **Automático, temporal**: rate limiter de login (`InMemoryLoginRateLimiter`, §6) —
  por IP y por usuario, se recupera solo con el tiempo (token bucket). No requiere
  intervención humana, cubre fuerza bruta/credential stuffing normal.
- **Manual, indefinido**: `POST /api/v1/usuarios/{id}/bloquear` (**Fase 4, nuevo** —
  el dominio (`Usuario.bloquear()`) existía desde antes de esta fase pero nunca
  estuvo expuesto por HTTP, confirmado al auditar `UsuarioController`). Marca
  `estado=BLOQUEADO`, revoca sesiones activas de inmediato (refresh tokens +
  access tokens ya emitidos vía `sver`). Pensado para sospecha de cuenta
  comprometida — reversible con `POST /{id}/activar`.

### Recuperación de contraseña olvidada

**Decisión: solo mediada por administrador, sin flujo de autoservicio por
correo.** `POST /api/v1/usuarios/{usuarioId}/password/restablecer` (§4) genera
una temporal, un ADMIN se la entrega al empleado por el canal que use la empresa
(no queda en ningún log ni respuesta salvo esa única vez). No hay
"forgot password" con link por correo. Motivo: la base de usuarios es personal
interno de confianza en una sola organización (no un público masivo
autoregistrado) — el costo/riesgo de un flujo de correo (spoofing, tokens de
reset que interceptar, infra de correo transaccional adicional) no se justifica
frente a "pedirle a un ADMIN que lo restablezca" cuando son unos pocos empleados
por tienda. Revisar esta decisión si la base de usuarios crece mucho o deja de
ser 100% personal interno.

### Baja de empleados

**Fase 4, nuevo** — antes de esta fase no existía ninguna forma de dar de baja a
un usuario vía la API (el dominio tenía `desactivar()` pero estaba muerto, sin
controller). Ahora:

- `POST /api/v1/usuarios/{id}/desactivar` — cese normal, `estado=INACTIVO`,
  revoca sesiones activas de inmediato. Reversible con `/activar`.
- Un usuario `INACTIVO`/`BLOQUEADO` no puede autenticarse (`Usuario.estaActivo()`,
  ya validado en `AuthServiceImpl.login`) ni seguir usando un token ya emitido
  (`sver` sube en la desactivación → `SecurityVersionValidator` lo rechaza en la
  próxima petición, no hace falta esperar a que expire).
- Permiso único `USUARIOS_CAMBIAR_ESTADO` para las 3 transiciones
  (desactivar/bloquear/activar) — es la misma clase de acción administrativa, no
  ameritan permisos separados.
- **Límite conocido, no resuelto acá**: no hay protección contra que un ADMIN se
  desactive/bloquee a sí mismo, ni contra quedarse sin ningún ADMIN activo (no se
  valida "¿queda al menos un ADMIN?" antes de aplicar el cambio). Bajo riesgo
  operativo hoy (equipos chicos, cambios manuales poco frecuentes) pero es una
  guardia real que falta si el equipo crece — agregar un chequeo de "no sos vos
  mismo" / "no sos el último ADMIN activo" antes de estas 3 transiciones si eso
  deja de ser cierto.

---

## 14. MFA — evaluación (Fase 4, PLAN_MEJORAS.md)

**Evaluado, no implementado** — el plan pide explícitamente "evaluar", no
construir; implementarlo es alcance de una fase aparte (nueva dependencia TOTP,
flujo de enrolamiento, códigos de respaldo, cambios en `AuthController`/login,
UI en backoffice y Flutter). Queda documentado acá para cuando se decida.

### Qué agregaría

Un segundo factor (TOTP — Google Authenticator/Authy/1Password, o WebAuthn/
passkeys) cierra el hueco real que sigue abierto hoy: si la contraseña de un
ADMIN se filtra (phishing, reuso de contraseña en otro sitio que sufrió una
brecha), no hay ninguna segunda barrera — Argon2id protege el hash en reposo,
no una contraseña ya conocida por el atacante.

### Con qué construirlo, si se decide

- **TOTP** (RFC 6238): librería `dev.samstevens.totp` (Java, sin dependencias
  pesadas) o `nimbus-jose-jwt`'s ecosystem no lo cubre — sería una dependencia
  nueva. Flujo: enrolar (generar secreto, mostrar QR, confirmar un código antes
  de activar) → login pasa a 2 pasos (`password` correcta → pedir TOTP → recién
  ahí emitir tokens) → códigos de respaldo de un solo uso (para cuando se pierde
  el dispositivo).
- **WebAuthn/passkeys**: más fricción de implementación (protocolo más nuevo,
  librería como `webauthn4j`), pero mejor UX (sin apps externas) y resistente a
  phishing por diseño (a diferencia de TOTP, que un sitio falso sí puede
  capturar y reenviar). Recomendado sobre TOTP si se va a invertir en esto,
  justamente por eso — pero más trabajo de integración.

### A quién aplicarlo primero

**Decisión pendiente del usuario, no técnica**: el plan menciona "administradores
y auditores" — tiene sentido exigirlo ahí primero (son las cuentas de mayor
impacto si se comprometen: `ADMIN` tiene alcance global, `AUDITOR` ve todo el
historial) antes de exigirlo a cajeros/encargados de tienda, donde la fricción
adicional en cada login pesa más frente al riesgo real (acceso limitado a su
propia tienda).

### Por qué no se implementó ya

Igual que la evaluación de logging remoto en `market-flutter` (ver su
`CLAUDE.md`) — agregar una dependencia nueva y un flujo que toca login en los 3
clientes (backend + backoffice + Flutter) es una decisión de producto, no algo
para decidir unilateralmente sin que el usuario elija el mecanismo (TOTP vs
WebAuthn) y confirme el alcance (solo ADMIN/AUDITOR, o todos).
</content>
