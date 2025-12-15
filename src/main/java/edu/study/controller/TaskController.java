package edu.study.controller;

import edu.study.model.Priority;
import edu.study.model.Task;
import edu.study.model.TaskStatus;
import edu.study.service.AnalyticsService;
import edu.study.service.TaskService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TaskController {
    private final TaskService taskService;
    private final AnalyticsService analyticsService;

    public TaskController(TaskService taskService, AnalyticsService analyticsService) {
        this.taskService = taskService;
        this.analyticsService = analyticsService;
    }

    public List<Task> loadTasks() {
        return taskService.listSortedTasks();
    }

    public Task createTask(String title, String description, Priority priority, LocalDateTime deadline,
                           Duration estimatedTime, String courseId, LocalDateTime startTime) {
        return taskService.addTask(title, description, priority, startTime, deadline, estimatedTime, courseId);
    }

    public Optional<Task> editTask(UUID id, String title, String description, Priority priority,
                                   LocalDateTime deadline, Duration estimatedTime, String courseId, LocalDateTime startTime) {
        return taskService.updateTask(id, title, description, priority, startTime, deadline, estimatedTime, courseId);
    }

    public Optional<Task> updateStatus(UUID id, TaskStatus status) {
        return taskService.updateStatus(id, status);
    }

    public boolean removeTask(UUID id) {
        return taskService.deleteTask(id);
    }

    public Map<LocalDate, Long> dailyStats() {
        return analyticsService.completionPerDay(taskService.listTasks());
    }

    public long doneTodayCount() {
        return analyticsService.countDoneToday(taskService.listTasks());
    }

    public boolean isHighRisk(Task task) {
        return analyticsService.isHighRisk(task, LocalDateTime.now());
    }

    public List<Task> tasksDueToday() {
        return taskService.tasksDueToday();
    }

    public List<Task> tasksWithSchedule() {
        return taskService.tasksWithSchedule();
    }

    public List<Task> tasksWithoutSchedule() {
        return taskService.tasksWithoutSchedule();
    }

    public void shutdown() {
        taskService.refreshStatuses();
        taskService.persist();
    }

    public void resetAll() {
        taskService.resetAll();
    }
}
