# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `2860ab74-29b7-46b3-9fd2-9815c92b826d`  
> - **Started:** 18/9/2026, 3:44:56 p.m.  
> - **Duration:** 54s  
> - **Exported:** 18/9/2026, 3:45:50 p.m.  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>7s</sub>

### Copilot

Reading the diff and the spec to gather all modified files and the expected contract. Opening both files in parallel to start the review.

---

<sub>7s</sub>

### `view`

**view the file at C:\Users\danie\taskflow-copilot-DanielDegollado4\semana6\proyecto-final.diff.**

C:\Users\danie\taskflow-copilot-DanielDegollado4\semana6\proyecto-final.diff

<details>
<summary>257 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..1606ada
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,31 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+/**
+ * ReportController — endpoints de reportes agregados.
+ */
+@RestController
+@Tag(name = "Reports", description = "Endpoints de reportes del sistema (avances, métricas)")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    @Operation(summary = "Avance por proyecto",
+            description = "Devuelve, para cada proyecto, cuántas tareas tiene, cuántas están DONE y el porcentaje completado redondeado a un decimal.")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..7968671
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,13 @@
+package com.taskflow.dto;
+
+/**
+ * ProjectProgressResponse — resumen de progreso por proyecto para GET /reports/progress.
+ *
+ * projectId: id del proyecto
+ * projectName: nombre del proyecto
+ * totalTasks: cantidad total de tareas del proyecto
+ * doneTasks: cuántas están en DONE
+ * percentDone: porcentaje completado (doneTasks * 100 / totalTasks), redondeado a 1 decimal
+ */
+public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index 39364f9..7c8ca15 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -32,4 +32,9 @@ public final class ProjectMapper {
                                                   long overdue) {
         return new ProjectSummaryResponse(p.getId(), p.getName(), totalTasks, byStatus, overdue);
     }
+
+    /** Proyecto + conteos -> DTO de salida de GET /reports/progress. */
+    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project p, long totalTasks, long doneTasks, double percentDone) {
+        return new com.taskflow.dto.ProjectProgressResponse(p.getId(), p.getName(), totalTasks, doneTasks, percentDone);
+    }
 }
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index db4698d..cefca4c 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -121,4 +121,27 @@ public class ProjectService {
         long vencidas = tareas.stream().filter(Task::estaVencida).count();
         return ProjectMapper.aResumen(proyecto, tareas.size(), porEstado, vencidas);
     }
+
+    /**
+     * Progreso por proyecto para GET /reports/progress: todos los proyectos, conteos y porcentaje
+     * redondeado a un decimal. Ordenado por projectId asc.
+     */
+    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
+        java.util.List<Project> proyectos = projectRepository.findAll();
+        return proyectos.stream()
+                .sorted(java.util.Comparator.comparing(Project::getId))
+                .map(p -> {
+                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
+                    long total = tareas.size();
+                    long done = tareas.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
+                    double percent;
+                    if (total == 0) {
+                        percent = 0.0;
+                    } else {
+                        percent = Math.round((done * 100.0 / total) * 10.0) / 10.0;
+                    }
+                    return ProjectMapper.aProgreso(p, total, done, percent);
+                })
+                .toList();
+    }
 }
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..e7b23df
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,54 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import com.taskflow.security.JwtAuthenticationFilter;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.boot.test.mock.mockito.MockBean;
+import org.springframework.test.context.junit.jupiter.SpringExtension;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@ExtendWith(SpringExtension.class)
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)
+public class ProgresoProyectosControllerTest {
+
+    @Autowired
+    MockMvc mockMvc;
+
+    @MockBean
+    ProjectService projectService;
+
+    @MockBean
+    JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getProgress_devuelve200_y_json_con_campos() throws Exception {
+        ProjectProgressResponse r1 = new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0);
+        ProjectProgressResponse r2 = new ProjectProgressResponse(2L, "App Móvil", 4, 1, 25.0);
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(r1, r2));
+
+        mockMvc.perform(get("/reports/progress"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].projectName").value("Plataforma TaskFlow"))
+                .andExpect(jsonPath("$[0].totalTasks").value(5))
+                .andExpect(jsonPath("$[0].doneTasks").value(1))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].projectId").value(2))
+                .andExpect(jsonPath("$[1].percentDone").value(25.0))
+                .andExpect(jsonPath("$[1].totalTasks").value(4));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..13a4a33
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,89 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.model.Priority;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Optional;
+
+import static org.assertj.core.api.Assertions.assertThat;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+public class ProgresoProyectosServiceTest {
+
+    @Mock
+    ProjectRepository projectRepository;
+
+    @Mock
+    TaskRepository taskRepository;
+
+    @Mock
+    UserRepository userRepository;
+
+    @InjectMocks
+    ProjectService projectService;
+
+    @Test
+    void progreso_por_proyecto_calcula_percentajes_y_orden() throws Exception {
+        Project p1 = new Project(1L, "Plataforma TaskFlow", "desc", 1L, LocalDate.now());
+        Project p2 = new Project(2L, "App Móvil", "desc", 1L, LocalDate.now());
+        Project p3 = new Project(3L, "Migración Legacy", "desc", 1L, LocalDate.now());
+
+        // p1: 5 tareas, 1 DONE
+        List<Task> tareasP1 = List.of(
+                new Task(10L, "t01", null, TaskStatus.DONE, Priority.MED, 1L, 1L, LocalDate.now()),
+                new Task(11L, "t02", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
+                new Task(12L, "t03", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
+                new Task(13L, "t04", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
+                new Task(14L, "t05", null, TaskStatus.TODO, Priority.MED, 1L, null, null)
+        );
+
+        // p2: 3 tareas, 1 DONE -> 33.3
+        List<Task> tareasP2 = List.of(
+                new Task(20L, "a01", null, TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now()),
+                new Task(21L, "a02", null, TaskStatus.TODO, Priority.MED, 2L, null, null),
+                new Task(22L, "a03", null, TaskStatus.TODO, Priority.MED, 2L, null, null)
+        );
+
+        // p3: no tareas
+        List<Task> tareasP3 = List.of();
+
+        // repo devuelve en orden 3,1,2 -> comprobamos que el servicio ordena 1,2,3
+        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));
+        when(taskRepository.findByProjectId(1L)).thenReturn(tareasP1);
+        when(taskRepository.findByProjectId(2L)).thenReturn(tareasP2);
+        when(taskRepository.findByProjectId(3L)).thenReturn(tareasP3);
+
+        List<ProjectProgressResponse> resultado = projectService.progresoPorProyecto();
+
+        assertThat(resultado).hasSize(3);
+        assertThat(resultado.get(0).projectId()).isEqualTo(1L);
+        assertThat(resultado.get(0).totalTasks()).isEqualTo(5);
+        assertThat(resultado.get(0).doneTasks()).isEqualTo(1);
+        assertThat(resultado.get(0).percentDone()).isEqualTo(20.0);
+
+        assertThat(resultado.get(1).projectId()).isEqualTo(2L);
+        assertThat(resultado.get(1).totalTasks()).isEqualTo(3);
+        assertThat(resultado.get(1).doneTasks()).isEqualTo(1);
+        assertThat(resultado.get(1).percentDone()).isEqualTo(33.3);
+
+        assertThat(resultado.get(2).projectId()).isEqualTo(3L);
+        assertThat(resultado.get(2).totalTasks()).isEqualTo(0);
+        assertThat(resultado.get(2).doneTasks()).isEqualTo(0);
+        assertThat(resultado.get(2).percentDone()).isEqualTo(0.0);
+    }
+}
```

</details>


---

<sub>11s</sub>

### Copilot

Abrir la especificación specs/progress.md para comparar contrato, campos y códigos HTTP con el diff; es necesario para checklist de revisión. Luego revisaré cada archivo modificado contra la spec. Ejecutando apertura del archivo specs/progress.md.

---

<sub>11s</sub>

### `view`

**view the file at C:\Users\danie\taskflow-copilot-DanielDegollado4\specs\progress.md.**

C:\Users\danie\taskflow-copilot-DanielDegollado4\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>54s</sub>

### Copilot

Resumen: comparé semana6/proyecto-final.diff con specs/progress.md. No hay problemas que bloqueen el merge; pequeños ajustes sugeridos.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/mapper/ProjectMapper.java | Convenciones / estilo | El método aProgreso usa el tipo totalmente calificado (com.taskflow.dto.ProjectProgressResponse) en lugar de import; funciona pero reduce claridad. | Importar ProjectProgressResponse y usar el tipo simple. |
| 2 | Sugerencia | src/main/java/com/taskflow/controller/ReportController.java | Javadoc / texto | Javadoc breve y correcto; comentario de clase genérico. | (Opcional) ampliar Javadoc para indicar código HTTP 200 esperado. |

Casos sin test:
- ninguno — los dos tests exigidos (unit y slice) están presentes y cubren orden, cálculo y respuesta.

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
