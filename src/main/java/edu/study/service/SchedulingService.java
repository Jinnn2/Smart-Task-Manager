package edu.study.service;

import edu.study.model.Task;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SchedulingService {
    private final Comparator<Task> schedulingComparator = Comparator
            .comparingInt((Task t) -> t.getPriority().getWeight()).reversed()
            .thenComparingLong(t -> t.remainingMinutes(LocalDateTime.now()))
            .thenComparingLong(t -> t.getEstimatedTime() != null ? t.getEstimatedTime().toMinutes() : Long.MAX_VALUE);

    public List<Task> sortTasks(List<Task> tasks) {
        return tasks.stream()
                .sorted(schedulingComparator)
                .collect(Collectors.toList());
    }
}
