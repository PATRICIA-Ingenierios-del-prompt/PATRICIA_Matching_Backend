# TODO — Integraciones pendientes para Matching

Este documento lista lo que falta en **usuarios-service** y **parches-core**
(según el código real de ambos repos, no supuestos) para que Matching pueda
dejar de correr con los perfiles `mock-usuarios` / `mock-parches` y operar
contra los servicios reales.

Generado al integrar Matching con:
- `PATRICIA_User_Backend-feature-inicial`
- `PATRICIA_Parches_Backend-feature-profilePhotoForParche`

---

## 1. Usuarios — falta un endpoint interno de perfil de matching

**Estado actual del repo de Usuarios:**
- `/internal/usuarios/{id}` (API key) devuelve `UsuarioResponse`: id, email,
  nombre, roles, estado. **No incluye carrera, semestre, intereses ni
  disponibilidad.**
- `/api/v1/usuarios/{id}/perfil`, `/intereses`, `/disponibilidad` sí tienen
  esos datos, pero exigen **JWT del propio usuario** (`JwtAuthenticationFilter`).
  No son utilizables para una llamada servicio-a-servicio: Matching no tiene
  (ni debería tener) el JWT de cada estudiante para consultarlo en su nombre.
- No existe ningún endpoint que liste varios usuarios/perfiles a la vez
  (necesario para construir el pool de candidatos).

**Lo que se necesita agregar a Usuarios** (bajo `/internal/usuarios/**`,
protegido con `InternalApiKeyFilter`, igual que los endpoints internos
existentes):

```
GET /internal/usuarios/{id}/perfil-matching
GET /internal/usuarios/candidatos-matching?excluirUsuarioId={id}&limite={n}
```

Respuesta propuesta (composición de `Usuario` + `Perfil`, ambos ya existen
en el dominio de Usuarios — solo falta el endpoint que los junte):

```json
{
  "id": "uuid",
  "estado": "ACTIVE",
  "intereses": ["Conciertos en vivo", "Grupos de estudio"],
  "carrera": "Ingenieria de Sistemas",
  "semestre": 5,
  "disponibilidad": "DISPONIBLE"
}
```

Implementación sugerida en Usuarios: un nuevo método en `PerfilService`
(o un servicio de aplicación nuevo) que combine
`usuarioRepository.buscarPorId(id)` + `perfilRepository.buscarPorUsuarioId(id)`,
expuesto en `InternalUsuarioController` (mismo controller que ya existe).
Para `candidatos-matching`, se necesita además una query de "varios
perfiles activos, paginada" — hoy `PerfilJpaRepository` solo tiene
`findByUsuarioId` (singular).

**Mientras tanto:** `PerfilUsuarioPort` se resuelve con
`MockPerfilUsuarioAdapter` (perfil Spring `mock-usuarios`).

---

## 2. Parches Core — falta toda la capa de consulta de membresías

**Estado actual del repo de Parches Core** (paquete `ingprompt.patricia.parches`):
- No existe ningún endpoint `/internal/**`; todo vive bajo `/api/parches/**`
  sin distinción interno/público y sin `X-Internal-Api-Key`.
- `ParcheRepositoryOutPort` solo tiene `findById(parcheId)`, `save`, `delete`.
  No hay ninguna query por miembro.
- Los miembros de un parche viven como `Set<UUID> members` embebido en el
  agregado `Parche` (probablemente `@ElementCollection` en
  `ParcheEntity`/`ParcheMapper` — no hay tabla de membresía independiente
  con índice por usuario).

**Lo que se necesita agregar a Parches Core** para soportar "parches en
común entre dos usuarios":

1. Una forma eficiente de responder "¿en qué parches está el usuario X?"
   — lo más simple es una tabla/índice de membresía separada
   (`parche_id, usuario_id`) en vez de la colección embebida actual, o al
   menos un `@Query` nativo sobre la colección si el volumen lo permite.
2. Un caso de uso nuevo, p. ej. `ParcheMembershipQueryCase.parchesDe(usuarioId)`.
3. Un controller interno nuevo (`/internal/parches/**`) con el mismo patrón
   de API key que ya usan Auth y Usuarios (Parches Core hoy no tiene
   ningún mecanismo de autenticación servicio-a-servicio — habría que
   introducirlo).

Endpoint propuesto:

```
GET /internal/parches/en-comun?usuarioA={a}&usuarioB={b}
-> { "parcheIds": ["uuid", "uuid"] }
```

**Mientras tanto:** `ParcheMembresiaPort` se resuelve con
`MockParcheMembresiaAdapter` (siempre devuelve conjunto vacío), y el bonus
de "parches en común" en el algoritmo de matching está **desactivado por
defecto** (`matching.parches-comun.habilitado=false`). El factor de 40/30/30
de RF05 sigue funcionando normalmente sin este bonus; es un ajuste
adicional, no un requisito del cálculo base.

---

## 3. Disponibilidad: limitación de modelo, no de integración

A diferencia de los dos puntos anteriores, esto **no se resuelve agregando
un endpoint** — es una decisión de producto ya tomada:

RF05.3 describe "match por disponibilidad" como cruce de franjas horarias
semanales. El modelo real de Usuarios (`enum Disponibilidad { DISPONIBLE,
OCUPADO, NO_MOLESTAR }`) es un estado puntual, no un horario. Matching
implementa el 30% de disponibilidad como un factor binario: 1.0 si ambos
usuarios están `DISPONIBLE` en el momento del cálculo, 0.0 en cualquier
otro caso (ver `CalculadoraCompatibilidad.scoreDisponibilidad`).

Si en el futuro Usuarios modela franjas horarias semanales, este factor
se puede reemplazar sin tocar los otros dos factores ni la ponderación
40/30/30: el cambio queda contenido en `CalculadoraCompatibilidad` y en
los campos que `PerfilMatching` trae de Usuarios.
