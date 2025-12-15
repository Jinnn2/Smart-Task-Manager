package edu.study.service;

import edu.study.model.Task;
import edu.study.model.TaskStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsService {

    public Map<LocalDate, Long> completionPerDay(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getCompletedAt() != null)
                .collect(Collectors.groupingBy(t -> t.getCompletedAt().toLocalDate(), Collectors.counting()));
    }

    public long countDoneToday(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().isEqual(today))
                .count();
    }

    public boolean isHighRisk(Task task, LocalDateTime now) {
        if (task.getStatus() == TaskStatus.DONE || task.getDeadline() == null) {
            return false;
        }
        Duration remaining = Duration.between(now, task.getDeadline());
        Duration estimated = task.getEstimatedTime() != null ? task.getEstimatedTime() : Duration.ofHours(1);
        return remaining.isNegative() || remaining.minus(estimated).isNegative();
    }
}
