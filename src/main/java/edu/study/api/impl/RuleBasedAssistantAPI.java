package edu.study.api.impl;

import edu.study.api.AssistantAPI;
import edu.study.model.Priority;
import edu.study.model.Task;
import edu.study.service.TaskService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RuleBasedAssistantAPI implements AssistantAPI {
    private final TaskService taskService;

    public RuleBasedAssistantAPI(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public String queryTaskSummary() {
        List<Task> tasks = taskService.listSortedTasks();
        if (tasks.isEmpty()) {
            return "当前没有待办任务。";
        }
        StringBuilder builder = new StringBuilder("任务概览：");
        for (Task task : tasks) {
            builder.append("\n- ").append(task.getTitle())
                    .append(" [").append(task.getPriority()).append("]")
                    .append(" 状态: ").append(task.getStatus());
            if (task.getDeadline() != null) {
                builder.append(" 截止: ").append(task.getDeadline());
            }
        }
        return builder.toString();
    }

    @Override
    public String queryTodayPlan() {
        List<Task> today = taskService.tasksDueToday();
        if (today.isEmpty()) {
            return "今天没有截止任务，专注补完积压或预习内容。";
        }
        return today.stream()
                .map(t -> t.getTitle() + "（优先级: " + t.getPriority() + "，预计耗时: " +
                        t.getEstimatedTime().toHours() + "h）")
                .collect(Collectors.joining("\n", "今日关键任务：\n", ""));
    }

    @Override
    public void addTaskFromNaturalLanguage(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        List<String> fragments = Arrays.stream(input.split("[,，。；;和]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        for (int i = 0; i < fragments.size(); i++) {
            String phrase = fragments.get(i);
            Priority priority = inferPriority(phrase);
            LocalDateTime deadline = LocalDateTime.now().withHour(23).withMinute(59).plusDays(i + 2);
            taskService.addTask(phrase, "从自然语言生成的任务", priority, null, deadline, Duration.ofHours(2), null);
        }
    }

    private Priority inferPriority(String phrase) {
        String lower = phrase.toLowerCase();
        if (lower.contains("考试") || lower.contains("midterm") || lower.contains("期中") || lower.contains("final")) {
            return Priority.CRITICAL;
        }
        if (lower.contains("论文") || lower.contains("essay") || lower.contains("paper")) {
            return Priority.HIGH;
        }
        if (lower.contains("预习") || lower.contains("准备")) {
            return Priority.MEDIUM;
        }
        return Priority.MEDIUM;
    }
}
