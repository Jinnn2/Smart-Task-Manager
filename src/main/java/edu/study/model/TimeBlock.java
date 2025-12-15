package edu.study.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimeBlock {
    private String id;
    private LocalDateTime start;
    private LocalDateTime end;
    private String taskId;
    private String description;

    public TimeBlock() {
    }

    public TimeBlock(LocalDateTime start, LocalDateTime end, UUID taskId, String description) {
        this.id = UUID.randomUUID().toString();
        this.start = start;
        this.end = end;
        this.taskId = taskId != null ? taskId.toString() : null;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
