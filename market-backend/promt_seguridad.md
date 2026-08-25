# Prompt para implementar el módulo de seguridad — Market Backend

Actúa como Arquitecto de Seguridad de Software y desarrollador senior especializado en
Java 25, Spring Boot 4, Spring Security, JWT, Jakarta Persistence y PostgreSQL.

Tu objetivo es diseñar, implementar, probar y documentar el módulo completo de
autenticación y autorización de `com.ais.marketbackend.seguridad`, dentro del modular
monolith descrito en `ARCHITECTURE.md` (capas `domain`/`application`/`infrastructure`/`api`).
Es un proyecto nuevo: no debes conservar esquemas criptográficos heredados ni introducir
mecanismos de compatibilidad con hashes antiguos. La solución debe aplicar principios de
mínimo privilegio, defensa en profundidad, configuración segura por defecto y denegación
ante cualquier ambigüedad.

Antes de modificar código:

1. Inspecciona el `pom.xml`, las entidades de `com.ais.marketbackend.seguridad`, la
   configuración existente y cualquier `AGENTS.md` aplicable.
2. Conserva los cambios no relacionados que ya existan en el repositorio.
3. Verifica las relaciones reales entre usuario, tienda, rol y permiso. No inventes
   relaciones que no estén respaldadas por las entidades o por el esquema.
4. Si el esquema actual no permite aplicar una regla de autorización sin ambigüedad,
   documenta el problema y crea la migración Liquibase mínima necesaria. No concedas
   acceso como solución alternativa.
5. Mantén la versión actual del proyecto: Spring Boot 4 y Java 25, salvo que exista una
   razón técnica comprobable para cambiarla.

## 1. Dependencias y alcance arquitectónico

Agrega únicamente las dependencias necesarias y compatibles con la versión administrada
por Spring Boot, incluyendo:

- Spring Security.
- Spring Security JOSE para JWT.
- Bean Validation.
- La implementación necesaria para Argon2id, si no está disponible en el classpath.
- Una solución de rate limiting apta para la arquitectura del proyecto.

No fijes versiones individuales cuando estén administradas por el BOM de Spring Boot.

Este backend autentica usuarios propios (personal administrativo y cajeros de tienda)
mediante un endpoint de login y emite un **access token JWT de corta duración** más un
**refresh token opaco** para renovarlo sin reautenticar. No presentes este login
propietario como si fuera OAuth 2.0/OIDC completo; si el negocio necesita interoperar con
un IdP externo (ej. login corporativo), indica que debe usarse un proveedor de identidad
externo aparte de este módulo.

No implementes manualmente la validación criptográfica del JWT mediante un filtro propio.
Usa los componentes estándar:

- `JwtEncoder` para emitir access tokens.
- `JwtDecoder` para validar access tokens.
- `JwtAuthenticationConverter` para convertir claims confiables en autoridades.
- `SecurityFilterChain` y método de autorización (`@PreAuthorize` o
  `AuthorizationManager<RequestAuthorizationContext>`) para autorizar solicitudes.

## 2. Autenticación y almacenamiento de contraseñas

No implementes `/auth/salt` ni hashing de contraseñas en el cliente.

El flujo obligatorio es:

1. El cliente envía `username` y contraseña mediante `POST /api/v1/auth/login`.
2. La solicitud viaja exclusivamente por HTTPS/TLS en todos los ambientes desplegados,
   excepto pruebas locales aisladas.
3. El servidor valida la contraseña mediante `PasswordEncoder.matches()`.
4. La contraseña se almacena exclusivamente mediante Argon2id y un sal aleatorio único
   gestionado por la implementación.
5. Nunca se almacena, registra, retorna ni propaga la contraseña en texto claro.

Usa `Argon2PasswordEncoder` con parámetros explícitos. Toma como mínimo de referencia
vigente de OWASP Argon2id `m=19456 KiB`, `t=2`, `p=1`, pero realiza una prueba de
rendimiento en el entorno del proyecto y ajusta el costo para que la verificación sea
deliberadamente lenta sin causar denegación de servicio. Documenta los parámetros finales
y su justificación.

Esquema mínimo (columnas indicativas, ajustar por migración Liquibase):

- `password_hash`: cadena codificada completa de Argon2id (algoritmo, parámetros, sal y hash).
- Restricción de unicidad para el username canónico.
- Ningún campo de sal separado ni columna para almacenar tokens de acceso.

Implementa una política de contraseñas orientada a longitud: frases largas permitidas,
longitud mínima configurable (≥ 12), longitud máxima razonable (≥ 64), todos los
caracteres Unicode y espacios permitidos, sin reglas arbitrarias de composición, sin
truncar ni normalizar silenciosamente.

## 3. Prevención de enumeración, timing attacks y ataques automatizados

Para `POST /api/v1/auth/login`:

- DTO JSON con validación estricta, límites de tamaño y rechazo de campos inesperados.
- Una única función de canonicalización del username, aplicada igual al alta y al login.
- Ante usuario inexistente, contraseña incorrecta o usuario inactivo/bloqueado: mismo
  código HTTP, mismo cuerpo genérico, encabezados equivalentes.
- Ejecuta `PasswordEncoder.matches()` con una credencial ficticia válida incluso cuando el
  usuario no exista, para no filtrar por tiempo.
- Emite el JWT únicamente después de completar todas las verificaciones de cuenta y de la
  tienda asignada.
- Devuelve `Cache-Control: no-store` y `Pragma: no-cache` en respuestas de autenticación.

Rate limiting funcional, no una anotación vacía:

- Límites por IP de origen validada y por hash HMAC del username canónico.
- Si existen múltiples instancias, almacenamiento compartido o solución distribuida.
- `429 Too Many Requests` con `Retry-After`, sin revelar existencia del usuario.
- Confía en `X-Forwarded-For` solo si la solicitud viene de un proxy confiable.
- Sin bloqueos permanentes por username (evita usarlo como DoS contra otra persona).

## 4. Configuración de Spring Security

- `SessionCreationPolicy.STATELESS`.
- Form login y HTTP Basic deshabilitados.
- Público exclusivamente `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` y un
  endpoint mínimo de salud expresamente autorizado.
- Rutas públicas restringidas por método HTTP y ruta exacta, no patrones amplios.
- Autenticación exigida para cualquier otra solicitud, política fail-closed.
- Resource Server con `JwtDecoder`.
- `AuthenticationEntryPoint`/`AccessDeniedHandler` producen JSON controlado: `401` ante
  credenciales ausentes/expiradas/inválidas, `403` ante permiso insuficiente.
- CORS con allowlist externa de orígenes/métodos/encabezados; nunca `*` con credenciales.
- CSRF deshabilitado porque el access token viaja en `Authorization: Bearer`; el refresh
  token, si se entrega en cookie, se sirve `HttpOnly`, `Secure`, `SameSite=Strict` y
  **reactiva CSRF** para el endpoint de refresh/logout.
- Encabezados seguros de Spring Security y HSTS en producción.
- Métodos HTTP no soportados y rutas ambiguas se rechazan.

No almacenes el access token en cookies. Documenta que un frontend web no debe guardar el
access token en `localStorage` si existe riesgo de XSS; recomienda un patrón BFF cuando la
arquitectura lo permita. El backoffice web (Vue) y el POS (Flutter) pueden diferir en cómo
guardan el refresh token — documenta la decisión para cada cliente por separado.

## 5. Emisión y validación segura de JWT

Firma asimétrica (RSA con SHA-256 o EC soportada). Las claves:

- Se generan fuera del código fuente.
- Se cargan desde un gestor de secretos, keystore o archivos montados de forma segura.
- No aparecen en Git, logs ni valores por defecto de producción.
- Soportan rotación mediante `kid` y un período controlado de coexistencia.

Fija el algoritmo permitido en la configuración. Nunca selecciones el algoritmo de
validación confiando en el encabezado `alg`. Rechaza `alg=none`, algoritmos inesperados,
claves desconocidas y tokens malformados.

Claims mínimas del **access token**: `sub` (identificador público del usuario), `iss`,
`aud`, `iat`, `nbf` (cuando aplique), `exp`, `jti`, y una claim propia con la tienda activa
del usuario cuando aplique al alcance de la operación. Tolerancia de reloj pequeña y
explícita. TTL corto, recomendado entre 5 y 15 minutos.

No incluyas contraseñas, hashes, sales, correos, nombres personales, IP ni datos sensibles
en el JWT. No registres tokens completos.

### Refresh token (obligatorio en este alcance)

- Opaco, aleatorio (no JWT), rotatorio y de un solo uso.
- Almacenado en servidor mediante **hash** (nunca en claro), con `usuario_id`, `expira_en`,
  `revocado`, `token_padre_id` (para detectar cadenas) y metadatos mínimos de dispositivo.
- `POST /api/v1/auth/refresh`: recibe el refresh token, valida hash + vigencia + no revocado,
  emite un access token nuevo **y un refresh token nuevo**, y revoca el anterior.
- **Detección de reutilización**: si un refresh token ya usado/revocado se presenta de
  nuevo, revoca toda la cadena de tokens de ese usuario y audita el evento como posible
  robo de token.
- `POST /api/v1/auth/logout` revoca el refresh token activo (y opcionalmente todos los del
  usuario). El access token JWT no se invalida (stateless): expira solo por TTL corto.
- TTL del refresh token configurable, considerablemente mayor que el del access token
  (ej. días), pero con revocación explícita disponible en todo momento.

## 6. Modelo de autorización (RBAC plano + alcance por tienda)

Modelo simplificado, sin bitstrings de acciones por opción:

- `Usuario`: identidad, estado de la cuenta, contraseña Argon2id.
- `Rol`: catálogo de roles de negocio (ej. `ADMIN`, `ENCARGADO_TIENDA`, `CAJERO`,
  `AUDITOR`).
- `Permiso`: catálogo de códigos de permiso planos, con convención
  `{MODULO}_{ACCION}` en MAYÚSCULAS_SNAKE (ej. `PRODUCTOS_VER`, `VENTAS_CREAR`,
  `CAJA_CERRAR`, `REPORTES_EXPORTAR`).
- `RolPermiso`: asignación many-to-many entre `Rol` y `Permiso` — el conjunto de permisos
  efectivos de un usuario es la unión de los permisos de todos sus roles asignados.
- `UsuarioTienda`: asignación de un usuario a una o varias tiendas, cada una con un rol.
  Un usuario sin asignación a una tienda no puede operar sobre los datos de esa tienda,
  independientemente de sus permisos globales — salvo un rol explícitamente marcado como
  multi-tienda (ej. `ADMIN`).

Regla de autorización:

```text
permitido = el usuario tiene, entre sus roles asignados (globales o de la tienda
            solicitada), un permiso con el código requerido por el endpoint,
            Y la tienda solicitada está entre las tiendas asignadas al usuario
            (o su rol es explícitamente multi-tienda).
```

- **Fail-closed**: cualquier ausencia, duplicado o dato de permiso corrupto produce
  denegación y un evento de auditoría — nunca acceso implícito.
- No autorices solo por rol ignorando el alcance de tienda cuando el recurso es
  tienda-específico (inventario, caja, ventas).
- Evita consultas N+1 y carga accidental de relaciones `LAZY`; usa una proyección o
  consulta dedicada que retorne solo los permisos efectivos y las tiendas asignadas.

## 7. Mapeo seguro entre endpoints y permisos

No infieras permisos por convención de URL (`startsWith`, segmentos de ruta). Usa una
anotación de método explícita, validada por Method Security:

```java
@RequiresPermission("VENTAS_CREAR")
```

- El código de permiso debe existir en el catálogo `Permiso`; si no existe, la aplicación
  no debe arrancar (falla el despliegue antes que exponer un endpoint sin permiso válido).
- Un controlador protegido sin `@RequiresPermission` queda igualmente autenticado por
  defecto (fail-closed en la capa HTTP), pero sin autorización de negocio — todo endpoint
  que mute o exponga datos de negocio debe anotarse explícitamente.
- Cuando el endpoint opera sobre un recurso de una tienda concreta, valida además el
  alcance de tienda del §6 — la presencia del permiso no basta para operar sobre
  cualquier tienda (equivalente a BOLA/IDOR a nivel de tienda).

## 8. Permisos en el JWT y vigencia

No incluyas el catálogo completo de permisos en el access token. Estrategia preferida:
JWT con identidad, roles y tienda(s) activa(s) mínimas; permisos efectivos consultados en
servidor con cache corta y correctamente invalidada. La base de datos es la fuente de
verdad: un cambio de rol o desactivación de cuenta debe poder surtir efecto antes de la
expiración normal del access token (TTL corto ya lo acota; una versión de seguridad del
usuario, incrementada en cambios críticos, permite invalidación más inmediata).

No confíes en claims sin firma válida. La presencia de una authority en el JWT no
reemplaza validaciones de propiedad del recurso ni de alcance de tienda.

## 9. Manejo de errores, auditoría y observabilidad

Formato JSON uniforme para errores, sin stack traces, nombres de tablas, consultas SQL,
claims internos ni razones que permitan enumerar usuarios.

Eventos de auditoría a distinguir internamente (ver `docs/auditoria.md` para el mecanismo
de entrega confiable vía outbox):

- Login exitoso o fallido.
- Rate limit alcanzado.
- Token inválido, expirado, con issuer/audience incorrectos.
- Reutilización de refresh token detectada (posible robo).
- Permiso o tienda denegados.
- Configuración o datos de permisos corruptos.
- Cambio de rol, permiso, tienda asignada o estado de cuenta.
- Rotación o clave JWT desconocida.

Incluye timestamp, correlation ID, tipo de evento, resultado y datos mínimos necesarios.
Evita log injection; no registres contraseñas, hashes, tokens, claves privadas ni datos
personales completos.

## 10. Pruebas obligatorias

Entrega pruebas unitarias y de integración con `MockMvc` o equivalente. Incluye como
mínimo:

### Autenticación

- Login correcto; username inexistente y contraseña incorrecta producen la misma
  respuesta externa; usuario inactivo/bloqueado no recibe token; Argon2id se ejecuta
  también para usuario inexistente; rate limiting por IP y username.

### Refresh token

- Refresh válido rota el token y emite nuevo access token.
- Refresh ya usado/revocado dispara revocación de toda la cadena.
- Refresh expirado se rechaza.
- Logout revoca el refresh activo.

### JWT (access token)

- Firma válida/inválida, expirado, aún no válido, `iss`/`aud` incorrectos, `alg=none`,
  `kid` desconocido, claims obligatorias ausentes, tolerancia de reloj dentro/fuera de
  límite.

### Autorización

- Cada permiso concede acceso solo al endpoint que lo requiere.
- Usuario sin el permiso requerido recibe `403`.
- Usuario con permiso pero sin asignación a la tienda solicitada recibe `403`.
- Rol multi-tienda opera sobre cualquier tienda.
- Unión de permisos cuando el usuario tiene múltiples roles.
- Permiso inexistente o catálogo corrupto falla de forma cerrada al arrancar.

### Configuración HTTP

- Solo login/refresh públicos; falta de token produce `401`; CORS acepta solo orígenes
  configurados; encabezados de seguridad esperados presentes.

No consideres terminada la implementación si las pruebas no compilan o fallan. Ejecuta el
conjunto completo de pruebas Maven y reporta el resultado.

## 11. Entregables

1. Dependencias Maven necesarias.
2. Migraciones Liquibase para usuarios, roles, permisos, asignación usuario-tienda-rol y
   refresh tokens (hash, no en claro).
3. DTOs validados para autenticación, refresh y respuestas.
4. Repositorios con consultas mínimas y parametrizadas.
5. `AuthController` y `AuthService` (login, refresh, logout).
6. Configuración de `PasswordEncoder` Argon2id.
7. `SecurityConfig` con Resource Server.
8. Configuración de `JwtEncoder`, `JwtDecoder`, validadores de claims y conversión de
   authorities.
9. Gestión externa y rotación de claves, sin secretos reales en el repositorio.
10. `@RequiresPermission` (o equivalente Method Security) para permiso y alcance de tienda.
11. Rate limiting funcional.
12. Manejo uniforme de `401`, `403`, `429` y errores de validación.
13. Auditoría y correlation ID.
14. Pruebas unitarias y de integración descritas anteriormente.
15. Ejemplo de configuración local con valores ficticios y variables de entorno, sin
    credenciales reales.
16. Un archivo `seguridad-desarrolladores.md`.

## 12. Criterios de aceptación y restricciones finales

- Compila y todas las pruebas pasan.
- No contiene secretos, claves privadas ni credenciales reales.
- No existe `/auth/salt` ni hashing rápido de contraseñas.
- No existe un filtro que reimplemente manualmente la validación JWT.
- `JwtDecoder` valida firma, algoritmo, `iss`, `aud`, tiempo y claims obligatorias.
- El refresh token es opaco, hasheado en servidor, rotatorio, de un solo uso y con
  detección de reutilización.
- Todas las rutas quedan protegidas por defecto; permiso y alcance de tienda fallan de
  forma cerrada.
- Los cambios de privilegios críticos pueden invalidar acceso antes de la expiración
  normal del access token.
- No se filtra información sensible mediante respuestas, URLs, logs o JWT.
- La documentación explica las decisiones, supuestos verificables y operación segura.

No reduzcas controles para hacer que una prueba pase. No crees algoritmos criptográficos
propios. Si una regla de negocio necesaria no puede inferirse del esquema, detén esa
parte, documenta exactamente la ambigüedad y solicita una decisión; mientras tanto,
conserva una denegación segura.
</content>
