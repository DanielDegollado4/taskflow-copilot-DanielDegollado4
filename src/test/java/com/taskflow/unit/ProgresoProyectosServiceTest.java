package com.taskflow.unit;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.model.Priority;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProgresoProyectosServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProjectService projectService;

    @Test
    void progreso_por_proyecto_calcula_percentajes_y_orden() throws Exception {
        Project p1 = new Project(1L, "Plataforma TaskFlow", "desc", 1L, LocalDate.now());
        Project p2 = new Project(2L, "App Móvil", "desc", 1L, LocalDate.now());
        Project p3 = new Project(3L, "Migración Legacy", "desc", 1L, LocalDate.now());

        // p1: 5 tareas, 1 DONE
        List<Task> tareasP1 = List.of(
                new Task(10L, "t01", null, TaskStatus.DONE, Priority.MED, 1L, 1L, LocalDate.now()),
                new Task(11L, "t02", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
                new Task(12L, "t03", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
                new Task(13L, "t04", null, TaskStatus.TODO, Priority.MED, 1L, null, null),
                new Task(14L, "t05", null, TaskStatus.TODO, Priority.MED, 1L, null, null)
        );

        // p2: 3 tareas, 1 DONE -> 33.3
        List<Task> tareasP2 = List.of(
                new Task(20L, "a01", null, TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now()),
                new Task(21L, "a02", null, TaskStatus.TODO, Priority.MED, 2L, null, null),
                new Task(22L, "a03", null, TaskStatus.TODO, Priority.MED, 2L, null, null)
        );

        // p3: no tareas
        List<Task> tareasP3 = List.of();

        // repo devuelve en orden 3,1,2 -> comprobamos que el servicio ordena 1,2,3
        when(projectRepository.findAll()).thenReturn(List.of(p3, p1, p2));
        when(taskRepository.findByProjectId(1L)).thenReturn(tareasP1);
        when(taskRepository.findByProjectId(2L)).thenReturn(tareasP2);
        when(taskRepository.findByProjectId(3L)).thenReturn(tareasP3);

        List<ProjectProgressResponse> resultado = projectService.progresoPorProyecto();

        assertThat(resultado).hasSize(3);
        assertThat(resultado.get(0).projectId()).isEqualTo(1L);
        assertThat(resultado.get(0).totalTasks()).isEqualTo(5);
        assertThat(resultado.get(0).doneTasks()).isEqualTo(1);
        assertThat(resultado.get(0).percentDone()).isEqualTo(20.0);

        assertThat(resultado.get(1).projectId()).isEqualTo(2L);
        assertThat(resultado.get(1).totalTasks()).isEqualTo(3);
        assertThat(resultado.get(1).doneTasks()).isEqualTo(1);
        assertThat(resultado.get(1).percentDone()).isEqualTo(33.3);

        assertThat(resultado.get(2).projectId()).isEqualTo(3L);
        assertThat(resultado.get(2).totalTasks()).isEqualTo(0);
        assertThat(resultado.get(2).doneTasks()).isEqualTo(0);
        assertThat(resultado.get(2).percentDone()).isEqualTo(0.0);
    }
}
