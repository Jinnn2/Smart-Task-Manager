package edu.study.service;

import edu.study.model.Priority;
import edu.study.model.Task;
import edu.study.model.TaskStatus;
import edu.study.repository.TaskRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TaskService {
    private final TaskRepository repository;
    private final SchedulingService schedulingService;
    private final List<Task> tasks = new ArrayList<>();

    public TaskService(TaskRepository repository, SchedulingService schedulingService) {
        this.repository = repository;
        this.schedulingService = schedulingService;
        this.tasks.addAll(repository.findAll());
        refreshStatuses();
    }

    public synchronized Task addTask(String title, String description, Priority priority, LocalDateTime startTime,
                                     LocalDateTime deadline, Duration estimatedTime, String courseId) {
        if (deadline == null) {
            throw new IllegalArgumentException("deadline is required");
        }
        Task task = new Task(title, description, priority, startTime, deadline, estimatedTime, courseId);
        task.refreshStatus(LocalDateTime.now());
        tasks.add(task);
        persist();
        return task;
    }

    public synchronized Optional<Task> updateTask(UUID taskId, String title, String description, Priority priority,
                                                  LocalDateTime startTime, LocalDateTime deadline, Duration estimatedTime, String courseId) {
        Optional<Task> existing = findById(taskId);
        existing.ifPresent(task -> {
            if (title != null) task.setTitle(title);
            if (description != null) task.setDescription(description);
            if (priority != null) task.setPriority(priority);
            if (startTime != null) task.setStartTime(startTime);
            if (deadline != null) task.setDeadline(deadline);
            if (estimatedTime != null) task.setEstimatedTime(estimatedTime);
            if (courseId != null) task.setCourseId(courseId);
            task.setUpdatedAt(LocalDateTime.now());
            if (task.getStatus() == TaskStatus.OVERDUE && task.getDeadline() != null
                    && task.getDeadline().isAfter(LocalDateTime.now())) {
                task.setStatus(TaskStatus.TODO);
            }
            task.refreshStatus(LocalDateTime.now());
            persist();
        });
        return existing;
    }

    public synchronized Optional<Task> updateStatus(UUID taskId, TaskStatus status) {
        Optional<Task> existing = findById(taskId);
        existing.ifPresent(task -> {
            if (status == TaskStatus.DONE) {
                task.markDone();
            } else {
                task.setStatus(status);
            }
            task.setUpdatedAt(LocalDateTime.now());
            persist();
        });
        return existing;
    }

    public synchronized boolean deleteTask(UUID taskId) {
        boolean removed = tasks.removeIf(t -> t.getTaskId() != null && t.getTaskId().equals(taskId));
        if (removed) {
            persist();
        }
        return removed;
    }

    public synchronized List<Task> listTasks() {
        refreshStatuses();
        return new ArrayList<>(tasks);
    }

    public synchronized List<Task> listSortedTasks() {
        refreshStatuses();
        return schedulingService.sortTasks(tasks);
    }

    public synchronized Optional<Task> findById(UUID id) {
        return tasks.stream()
                .filter(t -> t.getTaskId() != null && t.getTaskId().equals(id))
                .findFirst();
    }

    public synchronized List<Task> tasksDueToday() {
        refreshStatuses();
        LocalDate today = LocalDate.now();
        List<Task> dueToday = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getDeadline() != null && task.getDeadline().toLocalDate().isEqual(today)) {
                dueToday.add(task);
            }
        }
        return schedulingService.sortTasks(dueToday);
    }

    public synchronized List<Task> tasksWithSchedule() {
        refreshStatuses();
        List<Task> scheduled = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getStartTime() != null) {
                scheduled.add(task);
            }
        }
        return schedulingService.sortTasks(scheduled);
    }

    public synchronized List<Task> tasksWithoutSchedule() {
        refreshStatuses();
        List<Task> unscheduled = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getStartTime() == null) {
                unscheduled.add(task);
            }
        }
        unscheduled.sort((a, b) -> {
            int cmp = Integer.compare(b.getPriority().getWeight(), a.getPriority().getWeight());
            if (cmp != 0) return cmp;
            if (a.getDeadline() == null) return 1;
            if (b.getDeadline() == null) return -1;
            return a.getDeadline().compareTo(b.getDeadline());
        });
        return unscheduled;
    }

    public synchronized void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();
        tasks.forEach(t -> t.refreshStatus(now));
    }

    public synchronized void reload() {
        tasks.clear();
        tasks.addAll(repository.findAll());
        refreshStatuses();
    }

    public synchronized void persist() {
        repository.saveAll(tasks);
    }

    public synchronized void resetAll() {
        tasks.clear();
        persist();
    }
}
