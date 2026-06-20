# Matching Service — PATRICI.A

Microservicio de cálculo de compatibilidad y confirmación de matches entre
estudiantes (RF05). Construido en **arquitectura hexagonal** (puertos y
adaptadores) con Spring Boot 3.3.5 / Java 21, siguiendo las convenciones del
resto de la plataforma (ver `auth-service`).

> **Este repo fue verificado contra el código real de `usuarios-service`
> (rama `feature-inicial`) y `parches-core` (rama `feature-profilePhotoForParche`),
> no contra una especificación.** Dos endpoints internos que Matching necesita
> aún no existen en esos servicios; ver `TODO_INTEGRACIONES.md` para el
> detalle exacto y mientras tanto Matching corre con mocks (perfiles
> `mock-usuarios` / `mock-parches`).

## Responsabilidad y límites

**Matching SÍ hace:**
- Calcula compatibilidad entre usuarios: 40% intereses en común, 30%
  afinidad académica (carrera + semestre, con bonus opcional por parches
  en común — ver más abajo), 30% disponibilidad.
- Mantiene la cola de sugerencias por usuario en **Redis** (caché efímero,
  TTL configurable).
- Registra decisiones LIKE/DESCARTE; cuando dos usuarios se dan LIKE mutuo,
  confirma el match.
- Persiste matches confirmados en **PostgreSQL** (fuente de verdad de la
  relación de match).
- Publica `match.confirmado` a RabbitMQ.
- Escucha `usuario.actualizado`, `usuario.intereses.actualizados` y
  `disponibilidad.cambiada` (los tres reales, publicados por
  `UsuarioEventPublisherAdapter`) para recalcular sugerencias.

**Matching NO hace** (para no solapar con otros microservicios):
- No gestiona perfiles, intereses, fotos ni disponibilidad — eso es de
  **Usuarios**; Matching solo los consulta vía Feign.
- No gestiona grupos ni membresías — eso es de **Parches Core**; Matching
  solo consultaría parches en común vía Feign (hoy desactivado, ver abajo).
- No calcula ni usa geolocalización (RF05.2): queda fuera de este servicio.
- No envía notificaciones push — eso lo hace **Notificaciones**, que
  consume `match.confirmado`.

## Diferencias frente a RF05 tal como está redactado

1. **Disponibilidad no es horaria.** Usuarios modela `Disponibilidad` como
   un estado puntual (`DISPONIBLE`/`OCUPADO`/`NO_MOLESTAR`), no franjas
   semanales. El 30% de disponibilidad es binario: 1.0 si ambos están
   `DISPONIBLE` al momento del cálculo, 0.0 en cualquier otro caso. Ver
   `CalculadoraCompatibilidad` y la sección 3 de `TODO_INTEGRACIONES.md`.
2. **El bonus por parches en común está desactivado por defecto**
   (`matching.parches-comun.habilitado=false`). Parches Core no tiene hoy
   ninguna forma de consultar membresías por usuario (solo `findById(parcheId)`),
   así que no hay endpoint real al que llamar. El factor base 40/30/30 de
   RF05 funciona igual sin este bonus. Ver sección 2 de `TODO_INTEGRACIONES.md`.
3. **Usuarios no tiene "visibilidad de matching"** como campo de privacidad
   separado. La elegibilidad usa `EstadoUsuario == ACTIVE` (el filtro real
   que sí existe) en vez de un flag de visibilidad que no existe en el modelo.


- No valida JWT — confía en el header `X-User-Id` propagado por el Gateway
  tras la validación hecha por **Auth**.

## Arquitectura hexagonal

```
domain/
  model/        Entidades y value objects puros (Match, Sugerencia,
                 ScoreCompatibilidad, PerfilMatching, FranjaHoraria...)
                 + CalculadoraCompatibilidad (algoritmo RF05, sin
                 dependencias de framework).
  port/in/      Casos de uso (driving ports): CalcularSugerenciasUseCase,
                 ConsultarSugerenciasUseCase, DecidirSobreSugerenciaUseCase,
                 ListarMatchesUseCase.
  port/out/     Puertos de salida (driven ports): PerfilUsuarioPort,
                 ParcheMembresiaPort, ColaSugerenciasPort,
                 DecisionPendientePort, MatchRepositoryPort,
                 EventoMatchingPort.
  exception/    Excepciones de dominio.

application/
  service/      Implementación de los casos de uso, orquestando puertos.

infrastructure/
  adapter/in/web/         Controllers REST (driving adapters).
  adapter/in/messaging/   Listener de eventos de usuarios-service.
  adapter/out/cache/      Redis: cola de sugerencias + likes pendientes.
  adapter/out/persistence/ JPA/PostgreSQL: matches confirmados.
  adapter/out/client/     Feign hacia Usuarios y Parches Core.
  adapter/out/messaging/  Publisher de match.confirmado.
  mock/                   Mocks de Usuarios/Parches Core para desarrollo
                           aislado (perfiles mock-usuarios / mock-parches).
  config/, security/, exception/  Configuración transversal.
```

El dominio (`domain/`) no importa nada de Spring, Redis, JPA ni AMQP: solo
tipos de Java estándar. Eso permite testear el algoritmo de compatibilidad
y las reglas de match sin levantar contexto de Spring (ver `src/test`).

## Endpoints

| Método | Ruta                          | Descripción                                  |
|--------|-------------------------------|-----------------------------------------------|
| GET    | `/matching/sugerencias`       | Feed de candidatos (RF05.1)                   |
| POST   | `/matching/decisiones`        | LIKE/DESCARTE sobre un candidato              |
| GET    | `/matching/matches`           | Matches confirmados del usuario               |
| POST   | `/internal/matching/recalcular/{usuarioId}` | Fuerza recálculo (uso interno) |

Los endpoints `/matching/**` requieren el header `X-User-Id` (UUID).
Los endpoints `/internal/**` requieren `X-Internal-Api-Key`.

## Eventos

**Publica:**
- `match.confirmado` → consumido por Notificaciones (RF05.4) y por el
  propio feed de matches del usuario.

**Consume:**
**Consume:**
- `usuario.actualizado`, `usuario.intereses.actualizados`,
  `disponibilidad.cambiada` (de `patricia.usuarios`, publicados por
  `UsuarioEventPublisherAdapter` en usuarios-service) → recalcula la cola
  de sugerencias del usuario afectado.

## Correr en local

```bash
cp .env.example .env   # completar valores
docker compose up -d   # Redis (6380), RabbitMQ (5673/15673), Postgres (5433)
mvn spring-boot:run
```

### Desarrollo sin Usuarios / Parches Core desplegados

**Hoy esto no es opcional**: ni Usuarios ni Parches Core exponen todavía
los endpoints internos que Matching necesita (ver `TODO_INTEGRACIONES.md`).
Corre siempre con ambos perfiles activos hasta que se agreguen:

```bash
SPRING_PROFILES_ACTIVE=mock-usuarios,mock-parches mvn spring-boot:run
```

Esto reemplaza `FeignPerfilUsuarioAdapter` / `FeignParcheMembresiaAdapter`
por implementaciones en memoria con datos de prueba representativos del
catálogo real de intereses. Cuando los endpoints reales existan, retirar
los perfiles mock activa los adaptadores Feign automáticamente, sin tocar
el dominio ni la capa de aplicación (esa es la garantía que da la
arquitectura hexagonal) — ver `TODO_INTEGRACIONES.md` para el contrato
exacto que falta implementar en cada servicio.

## Tests

```bash
mvn test
```

Incluye tests de dominio puro (`CalculadoraCompatibilidadTest`,
`MatchTest`) y de aplicación con mocks de los puertos de salida
(`DecidirSobreSugerenciaServiceTest`).
