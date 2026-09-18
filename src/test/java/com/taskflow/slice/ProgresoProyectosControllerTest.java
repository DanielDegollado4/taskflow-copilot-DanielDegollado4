package com.taskflow.slice;

import com.taskflow.controller.ReportController;
import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.service.ProjectService;
import com.taskflow.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProgresoProyectosControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProjectService projectService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProgress_devuelve200_y_json_con_campos() throws Exception {
        ProjectProgressResponse r1 = new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5, 1, 20.0);
        ProjectProgressResponse r2 = new ProjectProgressResponse(2L, "App Móvil", 4, 1, 25.0);
        when(projectService.progresoPorProyecto()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/reports/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].projectName").value("Plataforma TaskFlow"))
                .andExpect(jsonPath("$[0].totalTasks").value(5))
                .andExpect(jsonPath("$[0].doneTasks").value(1))
                .andExpect(jsonPath("$[0].percentDone").value(20.0))
                .andExpect(jsonPath("$[1].projectId").value(2))
                .andExpect(jsonPath("$[1].percentDone").value(25.0))
                .andExpect(jsonPath("$[1].totalTasks").value(4));
    }
}
