package edu.study.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Task {
    private UUID taskId;
    private String title;
    private String description;
    private Priority priority = Priority.MEDIUM;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private TaskStatus status = TaskStatus.TODO;
    private Duration estimatedTime = Duration.ofHours(1);
    private String courseId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime completedAt;
    private int postponeCount = 0;

    public Task() {
        // for Jackson
    }

    public Task(String title, String description, Priority priority, LocalDateTime startTime, LocalDateTime deadline, Duration estimatedTime, String courseId) {
        this.taskId = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.startTime = startTime;
        this.deadline = deadline;
        this.estimatedTime = estimatedTime != null ? estimatedTime : Duration.ofHours(1);
        this.courseId = courseId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Duration getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Duration estimatedTime) {
        this.estimatedTime = estimatedTime != null ? estimatedTime : Duration.ofHours(1);
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getPostponeCount() {
        return postponeCount;
    }

    public void incrementPostponeCount() {
        this.postponeCount++;
    }

    public void setPostponeCount(int postponeCount) {
        this.postponeCount = postponeCount;
    }

    @JsonIgnore
    public boolean isOverdue(LocalDateTime now) {
        return deadline != null && now.isAfter(deadline) && status != TaskStatus.DONE;
    }

    public void refreshStatus(LocalDateTime now) {
        if (status == TaskStatus.DONE) {
            return;
        }
        if (isOverdue(now)) {
            status = TaskStatus.OVERDUE;
        }
    }

    public void markDone() {
        status = TaskStatus.DONE;
        completedAt = LocalDateTime.now();
    }

    @JsonIgnore
    public long remainingMinutes(LocalDateTime now) {
        if (deadline == null) {
            return Long.MAX_VALUE;
        }
        return Duration.between(now, deadline).toMinutes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(taskId, task.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}
