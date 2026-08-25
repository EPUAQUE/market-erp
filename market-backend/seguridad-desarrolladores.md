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

---

## 5. Access token y refresh token

- Firma **asimétrica RS256** para el access token. Componentes estándar: `JwtEncoder`
  (emisión), `JwtDecoder` (validación), sin filtro criptográfico propio.
- El algoritmo se **fija** en la configuración. **Nunca** se confía en el encabezado
  `alg`. Se rechaza `alg=none`, algoritmos inesperados, `kid` desconocido y tokens
  malformados.
- Claims del access token: `sub` (código público del usuario), `iss`, `aud`, `iat`,
  `nbf`, `exp`, `jti`, y `tiendas` (tiendas activas del usuario, cuando aplique).
  Tolerancia de reloj configurable (`clock-skew`, por defecto 30s). TTL corto (por
  defecto 10 min).
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

### Invalidación temprana (antes de `exp`)

Se usa la **versión de seguridad** del usuario (`version_seg`, claim `sver`). Se
revalida en cada petición contra la BD (con cache corta) que la cuenta siga activa y
que `sver` coincida. Para forzar la invalidación de todos los access tokens de un
usuario (cambio de contraseña, desactivación, cambio de rol/tienda): **incrementar
`version_seg`**. El desfase máximo es el TTL de la cache. El refresh token, además, se
revoca explícitamente en estos casos.

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
</content>
