package com.taskflow.dto;

/**
 * ProjectProgressResponse — resumen de progreso por proyecto para GET /reports/progress.
 *
 * projectId: id del proyecto
 * projectName: nombre del proyecto
 * totalTasks: cantidad total de tareas del proyecto
 * doneTasks: cuántas están en DONE
 * percentDone: porcentaje completado (doneTasks * 100 / totalTasks), redondeado a 1 decimal
 */
public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
}
