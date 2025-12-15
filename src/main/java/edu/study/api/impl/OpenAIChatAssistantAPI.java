package edu.study.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.study.api.AssistantAPI;
import edu.study.model.Priority;
import edu.study.model.Task;
import edu.study.service.TaskService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible Assistant API implementation.
 * Reads configuration from environment: OPENAI_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL.
 */
public class OpenAIChatAssistantAPI implements AssistantAPI {
    private final TaskService taskService;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RuleBasedAssistantAPI fallback;
    private final boolean debug = true;

    public OpenAIChatAssistantAPI(TaskService taskService, String apiKey, String baseUrl, String model) {
        this.taskService = taskService;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com/v1";
        this.model = model != null && !model.isBlank() ? model : "gpt-3.5-turbo";
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.fallback = new RuleBasedAssistantAPI(taskService);
    }

    @Override
    public String queryTaskSummary() {
        String prompt = buildSummaryPrompt(taskService.listSortedTasks());
        return callLLM(prompt, 2000).orElseGet(fallback::queryTaskSummary);
    }

    @Override
    public String queryTodayPlan() {
        String prompt = buildTodayPlanPrompt(taskService.tasksDueToday());
        return callLLM(prompt, 2000).orElseGet(fallback::queryTodayPlan);
    }

    @Override
    public void addTaskFromNaturalLanguage(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        String prompt = "You are a study task planner. From the description below, produce a JSON array of tasks. "
                + "JSON schema: [{\"title\": string, \"description\": string, \"priority\": one of [LOW,MEDIUM,HIGH,CRITICAL], "
                + "\"estimatedHours\": number (>=1), \"deadline\": ISO_LOCAL_DATE_TIME or ISO_LOCAL_DATE, "
                + "\"daysUntilDeadline\": integer (>=0, fallback if deadline missing)}]. "
                + "Return JSON first; notes (if any) go after JSON. "
                + "Description: \"" + input + "\"";
        Optional<String> result = callLLM(prompt, 3000);
        if (result.isEmpty()) {
            fallback.addTaskFromNaturalLanguage(input);
            return;
        }
        parseAndCreateTasks(result.get(), input);
    }

    private Optional<String> callLLM(String prompt, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            log("API key missing, fallback to rule-based.");
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.3);
            body.put("max_tokens", maxTokens);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(message("system", "You help students plan study tasks. Reply concisely in Chinese."));
            messages.add(message("user", prompt));
            body.put("messages", messages);

            String payload = mapper.writeValueAsString(body);
            log("LLM request => model=" + model + " url=" + baseUrl + "/chat/completions"
                    + " maxTokens=" + maxTokens + " prompt=" + truncate(prompt, 800)
                    + " payload=" + truncate(payload, 800));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String finishReason = extractFinishReason(response.body());
                log("LLM response status=" + response.statusCode() + " finish_reason=" + finishReason
                        + " body=" + truncate(response.body()));
                String content = extractContent(response.body());
                if (content == null || content.isBlank()) {
                    log("LLM response empty content. finish_reason=" + finishReason + " body=" + truncate(response.body()));
                    return Optional.empty();
                }
                return Optional.of(content);
            } else {
                log("LLM HTTP error code=" + response.statusCode() + " body=" + truncate(response.body()));
            }
        } catch (Exception e) {
            log("LLM call failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String buildSummaryPrompt(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "当前没有任务，请给一句简短鼓励。";
        }
        String list = tasks.stream()
                .map(t -> t.getTitle() + " | 状态:" + t.getStatus() + " | 优先级:" + t.getPriority()
                        + (t.getDeadline() != null ? " | 截止:" + t.getDeadline() : ""))
                .collect(Collectors.joining("\n"));
        return "根据以下任务，生成三行内的中文总结，标出最紧迫的两项（用序号），避免客套：\n" + list;
    }

    private String buildTodayPlanPrompt(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "今天没有截止任务，给出简短建议如何利用时间补完积压或预习。";
        }
        String list = tasks.stream()
                .map(t -> t.getTitle() + " | 优先级:" + t.getPriority() + " | 预计:" + t.getEstimatedTime().toHours() + "h")
                .collect(Collectors.joining("\n"));
        return "以下是今天截止或需要关注的任务，请生成一个可执行的时序计划（按时间顺序），中文回答，限制在5行内：\n" + list;
    }

    private void parseAndCreateTasks(String jsonText, String fallbackTitle) {
        try {
            List<?> raw = mapper.readValue(jsonText, List.class);
            for (Object obj : raw) {
                if (!(obj instanceof Map)) {
                    continue;
                }
                Map<?, ?> map = (Map<?, ?>) obj;
                String title = getString(map, "title", fallbackTitle);
                String description = getString(map, "description", "来自大模型的拆解");
                Priority priority = parsePriority(map.get("priority"));
                int days = parseInt(map.get("daysUntilDeadline"), 2);
                int hours = parseInt(map.get("estimatedHours"), 2);
                LocalDateTime deadline = parseDeadline(map.get("deadline"));
                if (deadline == null) {
                    deadline = LocalDateTime.now().plusDays(Math.max(days, 0)).withHour(23).withMinute(59);
                }
                taskService.addTask(title, description, priority, null, deadline, Duration.ofHours(Math.max(hours, 1)), null);
            }
        } catch (Exception e) {
            log("Failed to parse LLM tasks, fallback. err=" + e.getMessage());
            fallback.addTaskFromNaturalLanguage(fallbackTitle);
        }
    }

    private String getString(Map<?, ?> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object val = map.get(key);
        if (val == null) {
            return defaultValue;
        }
        String s = val.toString();
        return s.isBlank() ? defaultValue : s;
    }

    private Priority parsePriority(Object value) {
        if (value == null) {
            return Priority.MEDIUM;
        }
        String s = value.toString().trim().toUpperCase();
        switch (s) {
            case "CRITICAL":
                return Priority.CRITICAL;
            case "HIGH":
                return Priority.HIGH;
            case "LOW":
                return Priority.LOW;
            default:
                return Priority.MEDIUM;
        }
    }

    private int parseInt(Object value, int defaultValue) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDeadline(Object val) {
        if (val == null) {
            return null;
        }
        String s = val.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(s).atTime(23, 59);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractContent(String body) throws Exception {
        Map<?, ?> root = mapper.readValue(body, Map.class);
        Object choices = root.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
            return null;
        }
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map)) {
            return null;
        }
        Object message = ((Map<?, ?>) first).get("message");
        if (!(message instanceof Map)) {
            return null;
        }
        Object content = ((Map<?, ?>) message).get("content");
        return content != null ? content.toString().trim() : null;
    }

    private String extractFinishReason(String body) throws Exception {
        Map<?, ?> root = mapper.readValue(body, Map.class);
        Object choices = root.get("choices");
        if (!(choices instanceof List) || ((List<?>) choices).isEmpty()) {
            return null;
        }
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map)) {
            return null;
        }
        Object finishReason = ((Map<?, ?>) first).get("finish_reason");
        return finishReason != null ? finishReason.toString() : null;
    }

    private void log(String msg) {
        if (debug) {
            System.err.println("[LLM] " + msg);
        }
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 400 ? body.substring(0, 400) + "..." : body;
    }

    private String truncate(String body, int limit) {
        if (body == null) {
            return "";
        }
        return body.length() > limit ? body.substring(0, limit) + "..." : body;
    }
}
