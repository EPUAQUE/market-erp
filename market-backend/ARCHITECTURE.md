# Guía de arquitectura del proyecto — Market Backend

## 1. Objetivo

Market Backend es el núcleo de un **ERP Retail Multi-Tienda**: inventario, compras,
ventas, caja, créditos, gastos y facturación electrónica (FEL Guatemala) para varias
tiendas que venden catálogos de productos distintos entre sí.

La arquitectura es un **Modular Monolith** con **DDD** (Domain-Driven Design) y
**Clean Architecture** por módulo:

- Un solo desplegable (un solo proceso Spring Boot), pero con **fronteras de módulo
  estrictas**: cada módulo representa un contexto delimitado (bounded context) del
  negocio y solo expone su capa de aplicación al resto del sistema.
- Cada módulo se organiza en capas Clean Architecture (`domain → application →
  infrastructure/api`) para que las reglas de negocio no dependan de Spring, JPA ni HTTP.
- Esta separación permite, si el negocio crece, extraer un módulo a un servicio
  independiente sin reescribir su lógica de dominio (mismo principio que ya se aplica
  en `docs/auditoria.md` para separar el procesador de auditoría).

## 2. Paquete raíz y módulos

```text
com.ais.marketbackend
```

Cada módulo de negocio vive directamente bajo este paquete raíz:

| Módulo | Paquete | Responsabilidad de negocio |
| --- | --- | --- |
| Seguridad | `seguridad` | Usuarios, roles, permisos, autenticación JWT |
| Tiendas | `tiendas` | Catálogo de tiendas (sucursales) |
| Unidades de Medida | `unidadesmedida` | Catálogo de unidades |
| Categorías | `categorias` | Catálogo de categorías de producto |
| Marcas | `marcas` | Catálogo de marcas |
| Productos | `productos` | Catálogo global de productos y su configuración por tienda |
| Inventario | `inventario` | Existencias y kardex (movimientos de inventario) |
| Compras | `compras` | Órdenes de compra a proveedores |
| Proveedores | `proveedores` | Catálogo de proveedores |
| Cuentas por Pagar | `cuentasporpagar` | Saldos y pagos a proveedores |
| Clientes | `clientes` | Catálogo de clientes |
| Ventas | `ventas` | Ventas en tienda/POS |
| Cuentas por Cobrar | `cuentasporcobrar` | Saldos y pagos de clientes |
| Caja | `caja` | Apertura/cierre y movimientos de caja |
| Traslados | `traslados` | Movimiento de inventario entre tiendas |
| Gastos Programados | `gastosprogramados` | Gastos recurrentes (Quartz) |
| Notificaciones | `notificaciones` | Envío de correo/alertas (Spring Mail) |
| Facturación Electrónica FEL | `fel` | Integración con certificador FEL Guatemala |
| Dashboard | `dashboard` | Agregados de solo lectura para indicadores |
| Reportes | `reportes` | Exportación y reportes de negocio |

Los nombres de los paquetes se escriben siempre en minúsculas, sin guiones ni
guiones bajos.

## 3. Estructura obligatoria de un módulo

Cada módulo debe seguir la misma división de capas. No es necesario crear paquetes
vacíos: se agregan cuando el módulo realmente los utiliza.

```text
src/main/java/com/ais/marketbackend/
├── MarketBackendApplication.java
├── ventas/
│   ├── domain/
│   │   ├── model/            (agregados, entidades de dominio, value objects)
│   │   ├── repository/       (interfaces de persistencia — puertos)
│   │   └── event/             (eventos de dominio, ej. VentaRegistradaEvent)
│   ├── application/
│   │   ├── services/
│   │   │   ├── interfaces/    (casos de uso expuestos a otros módulos)
│   │   │   └── impl/          (orquesta dominio + repositorios; límite @Transactional)
│   │   └── dtos/               (comandos/resultados internos entre capas)
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── entities/       (entidades JPA — nunca el modelo de dominio)
│   │   │   ├── repositories/   (Spring Data JPA)
│   │   │   ├── adapters/       (implementan los puertos de domain.repository)
│   │   │   └── mappers/        (MapStruct: entidad JPA ↔ modelo de dominio)
│   │   └── external/           (adaptadores a sistemas externos, si aplica)
│   └── api/
│       ├── controllers/
│       ├── dtos/
│       │   ├── requests/
│       │   └── responses/
│       └── mappers/            (MapStruct: resultado de aplicación ↔ DTO HTTP)
└── shared/
    ├── kernel/                 (Value Objects reutilizables: Dinero, Nit, CodigoUnico…)
    ├── exceptions/
    ├── responses/
    └── config/
```

| Paquete | Responsabilidad |
| --- | --- |
| `domain.model` | Agregados y entidades con las reglas de negocio; sin anotaciones de Spring/JPA. |
| `domain.repository` | Contratos de persistencia en lenguaje de dominio (puertos). |
| `domain.event` | Eventos de dominio para desacoplar efectos secundarios dentro del monolito. |
| `application.services.interfaces` | Casos de uso del módulo; único punto de entrada permitido desde otros módulos. |
| `application.services.impl` | Orquesta agregados y repositorios; define transacciones; publica eventos. |
| `infrastructure.persistence.entities` | Entidades JPA (mapeo a tablas PostgreSQL). |
| `infrastructure.persistence.adapters` | Implementan `domain.repository` usando Spring Data JPA + MapStruct. |
| `infrastructure.external` | Adaptadores a proveedores externos (FEL, correo, pasarelas). |
| `api.controllers` | Expone endpoints HTTP; valida entrada; delega al servicio de aplicación. |
| `api.dtos.*` | Contrato HTTP de entrada/salida. |

## 4. Regla de dependencia entre capas (Clean Architecture)

```text
api  →  application  →  domain  ←  infrastructure
```

- `domain` no depende de nada del framework: sin `@Entity`, sin `@Service`, sin
  `jakarta.persistence`. Es Java puro con las reglas del negocio.
- `application` depende solo de `domain` (agregados + interfaces de repositorio).
  Define los casos de uso y el límite transaccional (`@Transactional`).
- `infrastructure` depende de `domain` para implementar sus puertos, y de librerías
  externas (Spring Data JPA, clientes HTTP, Quartz, Spring Mail).
- `api` depende solo de `application` (nunca de `infrastructure` ni de otro módulo
  directamente).
- Una entidad de persistencia (`infrastructure.persistence.entities`) o un agregado
  de dominio **nunca** se expone directamente en la API: los controllers reciben y
  devuelven DTOs de `api.dtos`.

## 5. Regla de dependencia entre módulos

Un módulo **solo** puede depender de `application.services.interfaces` de otro
módulo — nunca de su `domain`, `infrastructure` ni entidades JPA. Esto mantiene el
monolito extraíble a servicios independientes sin refactors profundos.

Integraciones síncronas esperadas (misma transacción, consistencia fuerte porque
mueven dinero/inventario):

| Módulo origen | Depende de (vía `services.interfaces`) | Motivo |
| --- | --- | --- |
| `ventas` | `inventario`, `cuentasporcobrar`, `caja`, `clientes`, `productos` | Registrar venta, kardex, crédito, cobro en caja |
| `compras` | `inventario`, `cuentasporpagar`, `proveedores`, `productos` | Registrar compra, kardex, crédito, historial de costo |
| `traslados` | `inventario` | Salida en tienda origen, entrada en tienda destino |

Integraciones asíncronas (eventos de dominio + patrón outbox, ver
`docs/auditoria.md` para el mecanismo de entrega confiable): `notificaciones` y
`fel` reaccionan a eventos como `VentaRegistradaEvent` sin bloquear la transacción
de venta. `gastosprogramados` corre en Quartz y llama a `cuentasporpagar`/
`notificaciones` por su interfaz pública, no por acceso directo a tablas.

`dashboard` y `reportes` son de **solo lectura**: consultan proyecciones o los
puertos de consulta de los demás módulos; nunca escriben en otro módulo.

## 6. Convenciones de nombres

| Tipo | Convención | Ejemplo |
| --- | --- | --- |
| Agregado / entidad de dominio | Nombre singular del negocio | `Venta`, `Producto`, `Inventario` |
| Value Object | Sustantivo del concepto | `Dinero`, `Nit`, `CodigoProducto` |
| Entidad JPA | Igual al agregado + sufijo si coexiste con el dominio | `VentaEntity` (si se requiere distinguir) |
| Repositorio (puerto, dominio) | `{Agregado}Repository` | `VentaRepository` |
| Repositorio Spring Data (infra) | `{Agregado}JpaRepository` | `VentaJpaRepository` |
| Adaptador de repositorio | `{Agregado}RepositoryAdapter` | `VentaRepositoryAdapter` |
| Interfaz de servicio de aplicación | `{Modulo}Service` | `VentaService` |
| Implementación | `{Modulo}ServiceImpl` | `VentaServiceImpl` |
| Controller | `{Modulo}Controller` | `VentaController` |
| DTO de entrada (API) | `{Accion}Request` | `RegistrarVentaRequest` |
| DTO de salida (API) | `{Accion}Response` | `VentaResponse` |
| Evento de dominio | `{Hecho}Event` | `VentaRegistradaEvent` |
| Excepción de negocio | `{Motivo}Exception` | `StockInsuficienteException` |

Clases/interfaces/enums en `PascalCase`; métodos y atributos en `camelCase`;
constantes en `UPPER_SNAKE_CASE`. Tablas y columnas PostgreSQL en `snake_case`
(ver §10).

## 7. Ejemplo — módulo `productos` (agregado simple, catálogo)

```java
package com.ais.marketbackend.productos.domain.model;

public class Producto {

    private final Long id;
    private String nombre;
    private String descripcion;
    private final Long categoriaId;
    private final Long marcaId;
    private final Long unidadMedidaId;
    private boolean activo;

    public Producto(Long id, String nombre, String descripcion,
                     Long categoriaId, Long marcaId, Long unidadMedidaId) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.unidadMedidaId = unidadMedidaId;
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    // getters, sin setters públicos salvo intención de negocio explícita
}
```

```java
package com.ais.marketbackend.productos.domain.repository;

import com.ais.marketbackend.productos.domain.model.Producto;
import java.util.Optional;

public interface ProductoRepository {

    Producto save(Producto producto);

    Optional<Producto> findById(Long id);

    boolean existsByCodigoInterno(String codigoInterno);
}
```

```java
package com.ais.marketbackend.productos.application.services.impl;

import com.ais.marketbackend.productos.application.services.interfaces.ProductoService;
import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public Producto crear(Producto producto) {
        if (productoRepository.existsByCodigoInterno(producto.getCodigoInterno())) {
            throw new CodigoProductoDuplicadoException(producto.getCodigoInterno());
        }
        return productoRepository.save(producto);
    }
}
```

La entidad JPA vive aparte, en `infrastructure.persistence.entities`, y un
`ProductoRepositoryAdapter` en `infrastructure.persistence.adapters` traduce entre
`ProductoEntity` (JPA) y `Producto` (dominio) usando un mapper de MapStruct.

## 8. Ejemplo — invariante de dominio en `inventario` (kardex)

Regla de negocio: **nunca se modifica `existenciaActual` directamente; todo cambio
nace de un `MovimientoInventario`.**

```java
package com.ais.marketbackend.inventario.domain.model;

import java.math.BigDecimal;

public class Inventario {

    private final Long tiendaId;
    private final Long productoId;
    private BigDecimal existenciaActual;
    private BigDecimal costoPromedioActual;

    public Inventario(Long tiendaId, Long productoId,
                       BigDecimal existenciaActual, BigDecimal costoPromedioActual) {
        this.tiendaId = tiendaId;
        this.productoId = productoId;
        this.existenciaActual = existenciaActual;
        this.costoPromedioActual = costoPromedioActual;
    }

    /** Único punto de mutación: aplica un movimiento y recalcula costo promedio. */
    public void aplicar(MovimientoInventario movimiento) {
        switch (movimiento.getTipo()) {
            case COMPRA, AJUSTE_POSITIVO, TRASLADO_ENTRADA, DEVOLUCION_CLIENTE ->
                    ingresar(movimiento.getCantidad(), movimiento.getCostoUnitario());
            case VENTA, AJUSTE_NEGATIVO, TRASLADO_SALIDA, DEVOLUCION_PROVEEDOR ->
                    egresar(movimiento.getCantidad());
        }
    }

    private void ingresar(BigDecimal cantidad, BigDecimal costoUnitario) {
        BigDecimal valorActual = existenciaActual.multiply(costoPromedioActual);
        BigDecimal valorEntrante = cantidad.multiply(costoUnitario);
        BigDecimal nuevaExistencia = existenciaActual.add(cantidad);
        this.costoPromedioActual = valorActual.add(valorEntrante)
                .divide(nuevaExistencia, 4, java.math.RoundingMode.HALF_UP);
        this.existenciaActual = nuevaExistencia;
    }

    private void egresar(BigDecimal cantidad) {
        if (existenciaActual.compareTo(cantidad) < 0) {
            throw new StockInsuficienteException(productoId, tiendaId);
        }
        this.existenciaActual = existenciaActual.subtract(cantidad);
    }
}
```

`InventarioServiceImpl.registrarMovimiento(...)` es el **único** método público que
otros módulos (`ventas`, `compras`, `traslados`) pueden invocar: internamente crea el
`MovimientoInventario` (persistido append-only, igual que `AUDIT_EVENT` en
`docs/auditoria.md`), carga el `Inventario` agregado, aplica el movimiento y
persiste ambos en la misma transacción. Antes de aceptar un movimiento de entrada
(`COMPRA`, `AJUSTE_POSITIVO`, `TRASLADO_ENTRADA`) valida contra `productos` que
`ProductoTienda.permitirIngreso = true`; si es `false`, rechaza la operación con una
excepción de negocio — el producto sigue siendo vendible con la existencia que ya
tenga.

## 9. DTOs (capa `api`)

Los DTOs constituyen el contrato HTTP. Inmutables cuando sea posible; nunca se
reutiliza una entidad JPA ni un agregado de dominio como DTO.

```java
package com.ais.marketbackend.ventas.api.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegistrarVentaDetalleRequest {

    @NotNull
    Long productoId;

    @Positive
    java.math.BigDecimal cantidad;
}
```

En Java 25 también pueden usarse `record` para DTOs simples de una sola capa; elige
una convención por tipo de DTO y mantenla en todo el módulo.

## 10. Persistencia PostgreSQL y Liquibase

- Motor: **PostgreSQL**. Tablas y columnas en `snake_case` (`producto_tienda`,
  `movimiento_inventario`, `cuenta_por_cobrar`).
- Migraciones con **Liquibase**, un changelog maestro que incluye un changelog por
  módulo:

```text
src/main/resources/db/changelog/
├── db.changelog-master.xml
└── modules/
    ├── productos/001-create-productos.xml
    ├── inventario/001-create-inventario.xml
    └── ventas/001-create-ventas.xml
```

- Un changeset por cambio de esquema, nunca editar un changeset ya aplicado en un
  ambiente compartido — se agrega uno nuevo.
- `GenerationType.IDENTITY` (soportado nativamente por PostgreSQL) para claves
  primarias `Long`, salvo que un agregado requiera un identificador público
  independiente del autoincremental (ej. `codigoInterno` de `Producto`).
- Restricciones de negocio (unicidad, `CHECK`) se declaran en el changelog, no solo
  en Bean Validation — la base es la última línea de defensa.

## 11. Repositories

Puerto en `domain.repository` (lenguaje de dominio, sin `JpaRepository`), adaptador
en `infrastructure.persistence.adapters` que sí usa Spring Data JPA:

```java
package com.ais.marketbackend.productos.infrastructure.persistence.repositories;

import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoJpaRepository extends JpaRepository<ProductoEntity, Long> {

    boolean existsByCodigoInterno(String codigoInterno);
}
```

```java
package com.ais.marketbackend.productos.infrastructure.persistence.adapters;

import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.domain.repository.ProductoRepository;
import com.ais.marketbackend.productos.infrastructure.persistence.mappers.ProductoEntityMapper;
import com.ais.marketbackend.productos.infrastructure.persistence.repositories.ProductoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductoRepositoryAdapter implements ProductoRepository {

    private final ProductoJpaRepository jpaRepository;
    private final ProductoEntityMapper mapper;

    @Override
    public Producto save(Producto producto) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(producto)));
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper.toDomain());
    }

    @Override
    public boolean existsByCodigoInterno(String codigoInterno) {
        return jpaRepository.existsByCodigoInterno(codigoInterno);
    }
}
```

## 12. MapStruct

MapStruct traduce entre las tres representaciones de un mismo concepto (entidad
JPA, agregado de dominio, DTO HTTP) para que ninguna capa dependa de otra hacia
arriba:

```java
package com.ais.marketbackend.productos.infrastructure.persistence.mappers;

import com.ais.marketbackend.productos.domain.model.Producto;
import com.ais.marketbackend.productos.infrastructure.persistence.entities.ProductoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductoEntityMapper {

    Producto toDomain(ProductoEntity entity);

    ProductoEntity toEntity(Producto domain);
}
```

Un segundo mapper en `api.mappers` traduce `Producto` (dominio) ↔
`ProductoResponse`/`CrearProductoRequest` (DTO HTTP). No se combinan ambos mappers
en uno solo: cada capa mapea solo hacia su vecino inmediato.

## 13. Manejo de errores

Las excepciones de negocio son específicas (`StockInsuficienteException`,
`CodigoProductoDuplicadoException`, …) y se traducen a una respuesta HTTP
consistente desde `@RestControllerAdvice` en `shared.exceptions`:

```text
shared/
├── exceptions/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── BusinessException.java
└── responses/
    └── ApiErrorResponse.java
```

El error de la API incluye, como mínimo: marca de tiempo, código HTTP, mensaje
legible, ruta e identificador de error. Nunca se devuelven trazas de ejecución ni
mensajes de PostgreSQL al cliente.

## 14. Uso recomendado de Lombok

| Anotación | Uso recomendado |
| --- | --- |
| `@RequiredArgsConstructor` | Inyección por constructor en controllers, servicios de aplicación y adaptadores. |
| `@Value` | DTOs inmutables (capa `api`). |
| `@Builder` | Construcción legible de DTOs u objetos con varios atributos. |
| `@Getter` | Lectura controlada de entidades JPA. |
| `@NoArgsConstructor` | Solo en entidades JPA, con el acceso más restrictivo posible. |
| `@Slf4j` | Logging cuando la clase realmente necesita registrar eventos. |

Evitar: `@Data` en entidades JPA o en agregados de dominio, `@Setter` a nivel de
clase, `@SneakyThrows`, `@EqualsAndHashCode` incluyendo asociaciones JPA. Los
agregados de dominio (`domain.model`) no llevan anotaciones Lombok orientadas a
persistencia — son objetos Java simples con métodos de negocio.

## 15. Pruebas

Las pruebas reflejan la misma estructura de capas del módulo:

```text
src/test/java/com/ais/marketbackend/
└── inventario/
    ├── domain/
    │   └── model/InventarioTest.java          (invariantes, sin Spring)
    ├── application/
    │   └── services/InventarioServiceImplTest.java
    ├── infrastructure/
    │   └── persistence/InventarioRepositoryAdapterTest.java
    └── api/
        └── controllers/InventarioControllerTest.java
```

- `domain`: pruebas unitarias puras (sin `@SpringBootTest`) de las reglas de
  negocio — costo promedio, stock insuficiente, `permitirIngreso`.
- `application`: pruebas de orquestación con repositorios y dependencias mockeadas.
- `infrastructure`: pruebas de integración de las consultas (Testcontainers +
  PostgreSQL).
- `api`: pruebas del contrato HTTP con `MockMvc`.

Nombres de prueba describen el comportamiento esperado, ej.:
`shouldRejectVentaWhenStockInsuficiente`.

## 16. Dependencias necesarias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-liquibase</artifactId>
</dependency>
<dependency>
    <groupId>org.quartz-scheduler</groupId>
    <artifactId>quartz</artifactId>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

`mapstruct-processor` se declara en `maven-compiler-plugin` junto con
`lombok-mapstruct-binding` para que Lombok y MapStruct convivan en la misma clase
cuando sea necesario. No fijar versiones individuales cuando estén administradas
por el BOM de Spring Boot.

## 17. Lista de verificación para un nuevo módulo

Antes de considerar completo un módulo, verificar que:

- Está ubicado bajo `com.ais.marketbackend.<modulo>` con las cuatro capas
  (`domain`, `application`, `infrastructure`, `api`).
- El `domain.model` no importa nada de `jakarta.persistence`, Spring ni HTTP.
- El controller solo gestiona HTTP y delega al servicio de aplicación.
- Otro módulo, si lo necesita, solo importa `application.services.interfaces` de
  este módulo — nunca su `domain`, `infrastructure` ni entidades JPA.
- Las entidades JPA y los agregados de dominio no se exponen en la API.
- Los movimientos de estado sensibles (inventario, caja, cuentas) pasan por un
  único método de aplicación que registra el hecho (kardex/movimiento) antes de
  mutar el agregado — nunca un `setter` público directo.
- Existen migraciones Liquibase para el esquema nuevo, con restricciones a nivel
  de base de datos para las reglas críticas.
- Existen pruebas para el dominio, la orquestación de aplicación y el contrato HTTP.
- No se registran contraseñas, tokens ni información sensible en logs.
</content>
