# Arquitectura de TaskFlow

Este documento orienta a un desarrollador nuevo.

1) Capas y paquetes

- Paquete raíz: `com.taskflow`.
- Controller (HTTP): `src/main/java/com/taskflow/controller/` — clases: `TaskController`, `ProjectController`, `AuthController`, `InfoController`.
- Service (casos de uso): `src/main/java/com/taskflow/service/` — ejemplos: `TaskService`, `ProjectService`, `AuthService`, `ReportService`.
- Repository (persistencia): `src/main/java/com/taskflow/repository/` — `TaskRepository`, `ProjectRepository`, `UserRepository` (extienden `JpaRepository`).
- Model (entidades/negocio): `src/main/java/com/taskflow/model/` — `Task`, `Project`, `User`, `TaskStatus`, `Priority`.
- DTOs y mappers: `src/main/java/com/taskflow/dto/` y `src/main/java/com/taskflow/mapper/` — `TaskRequest`, `TaskResponse`, `TaskMapper`, `ProjectMapper`.
- Seguridad: `src/main/java/com/taskflow/security/` y `src/main/java/com/taskflow/config/` — `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `ProjectSecurity`.
- Excepciones y advice: `src/main/java/com/taskflow/exception/` y `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`.

2) Recorrido de POST /projects/{projectId}/tasks

- Request llega a `src/main/java/com/taskflow/controller/TaskController.java` en el método `createTask` (ruta `/projects/{projectId}/tasks`).
- El `controller` valida que el `projectId` exista consultando a `ProjectService` (`src/main/java/com/taskflow/service/ProjectService.java`). Si no existe lanza `ProjectNotFoundException` -> 404.
- Llama a `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
- Dentro de `TaskService.crear` el `TaskMapper` (`src/main/java/com/taskflow/mapper/TaskMapper.java`) transforma el `TaskRequest` + `projectId` en una entidad nueva llamando a la factory de negocio `Task.crear(...)` (`src/main/java/com/taskflow/model/Task.java`).
- `Task.crear` aplica reglas de negocio de creación (por ejemplo: `dueDate` no puede estar en el pasado) y fija `status = TODO`.
- El `Task` resultante se persiste con `TaskRepository.save(...)` (`src/main/java/com/taskflow/repository/TaskRepository.java`), que traduce a una operación INSERT en la base de datos (JPA/Hibernate).
- `TaskController` construye `Location` con `/tasks/{id}` y devuelve `201 Created` con el `TaskResponse` mapeado por `TaskMapper.aResponse`.

Diagrama simple (secuencia):
`HTTP POST /projects/{projectId}/tasks` -> `TaskController.createTask` -> `ProjectService.buscarPorId` (404 si no) -> `TaskService.crear` -> `TaskMapper.aEntidadNueva` -> `Task.crear` (reglas) -> `TaskRepository.save` -> DB

3) Dónde viven las reglas de negocio

- Reglas de validación e invariantes de dominio viven en la entidad `Task` (`src/main/java/com/taskflow/model/Task.java`) — por ejemplo:
  - `Task.crear(...)` verifica `dueDate` y fija `status` inicial.
  - El constructor de rehidratación valida título y `projectId`.
  - `Task.setStatus(...)` contiene la regla «no pasar a DONE sin `assigneeId`».
- El servicio `TaskService` orquesta casos de uso y traduce excepciones de dominio a excepciones de aplicación (`TaskValidationException` -> handler, `TaskStateException` -> 422) pero NO duplica la lógica del dominio.
- Mapeos y diferencias crear vs rehidratación se implementan en `TaskMapper`.

4) Cómo funciona la seguridad (JWT)

- Configuración central: `src/main/java/com/taskflow/config/SecurityConfig.java` — define la `SecurityFilterChain`, reglas de rutas públicas (`/auth/**`, `/info`, Swagger, H2) y `SessionCreationPolicy.STATELESS`.
- Emisión de tokens: `src/main/java/com/taskflow/security/JwtService.java` genera y valida JWTs (claims: `sub` = username, `role`, `iat`, `exp`). El secret debe venir de configuración/variable de entorno.
- Autenticación por request: `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` inspecciona el header `Authorization: Bearer <token>`; si existe valida la firma y, si es válida, carga `UserDetails` (la tabla `users` vía `JpaUserDetailsService`) y pone la `Authentication` en el `SecurityContext`.
- Resultado: endpoints protegidos requieren token (sin token -> 401 JSON). Autorización fina se hace con `@PreAuthorize` y la ayuda de `ProjectSecurity` para reglas como «solo owner o ADMIN puede borrar un proyecto».

5) Organización de los tests

- Unit tests: `src/test/java/com/taskflow/unit/` — JUnit + Mockito, prueba lógica aislada (por ejemplo `TaskServiceTest`, `TaskValidationTest`). Se ejecutan con `mvn -q test`.
- Slice tests (tests por capa): `src/test/java/com/taskflow/slice/` — `@WebMvcTest` / `@DataJpaTest` para probar controladores y repositorios en aislamiento (H2 en memoria en slices cuando aplica).
- Integration / IT: `src/test/java/com/taskflow/integration/` — `@SpringBootTest` y pruebas end-to-end. Algunos `*IT.java` usan Testcontainers (Postgres) y no se ejecutan con la invocación por defecto; para ejecutarlos hay que activar `-Ddocker.tests=true` según la política del proyecto.
- Comandos útiles:
  - Ejecutar la suite normal: `mvn -q test`
  - Ejecutar una sola clase: `mvn -q test "-Dtest=TaskServiceTest"`
  - Ejecutar con profile H2 (arranca datos de ejemplo): `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"`

6) Notas prácticas rápidas

- Controllers nunca devuelven entidades; siempre DTOs (`TaskRequest`/`TaskResponse`).
- JPA rehidrata con constructor no-arg; la CREACIÓN de negocio debe pasar por las fábricas estáticas (`Task.crear`).
- No duplicar reglas en ambos lados (servicio vs entidad): la regla tiene prioridad en la entidad.
- Mensajes de error y códigos (400/404/422/409/401/403) están centralizados en `GlobalExceptionHandler` (`src/main/java/com/taskflow/advice/GlobalExceptionHandler.java`).

Si quieres, puedo añadir un diagrama mermaid o un ejemplo paso-a-paso con fragmentos de código para `POST /projects/{projectId}/tasks`.
Las validaciones de fecha límite se realizan en la fábrica de la entidad `Task` — `Task.crear(...)` (`src/main/java/com/taskflow/model/Task.java`).
