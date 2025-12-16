package edu.study.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.study.api.ChatClient;
import edu.study.model.PersonalProfile;
import edu.study.model.Task;
import edu.study.util.LlmLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OpenAIChatClient implements ChatClient {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final boolean debug = true;

    public OpenAIChatClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://api.openai.com/v1";
        this.model = model != null && !model.isBlank() ? model : "gpt-4o";
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public Optional<String> chat(String userMessage, List<Task> tasks, PersonalProfile profile) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String payload = null;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", 0.5);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(message("system", buildSystemPrompt(profile, tasks)));
            messages.add(message("user", userMessage));
            body.put("messages", messages);
            payload = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (debug) {
                System.err.println("[CHAT] status=" + response.statusCode() + " body=" + truncate(response.body(), 600));
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LlmLogger.log("chat", payload, response.body());
                return Optional.ofNullable(extractContent(response.body()));
            } else {
                LlmLogger.log("chat-error", payload, response.body());
            }
        } catch (Exception e) {
            if (debug) {
                System.err.println("[CHAT] failed: " + e.getMessage());
            }
            try {
                LlmLogger.log("chat-error", payload != null ? payload : "payload-none", e.toString());
            } catch (Exception ignored) {
            }
        }
        return Optional.empty();
    }

    private String buildSystemPrompt(PersonalProfile profile, List<Task> tasks) {
        StringBuilder sb = new StringBuilder("你是学习计划助手，基于提供的个人信息和日程，用简体中文给出简洁可执行的回复。\n");
        if (profile != null) {
            sb.append("个人信息: ");
            if (profile.getName() != null) sb.append("姓名:").append(profile.getName()).append("; ");
            if (profile.getMajor() != null) sb.append("专业:").append(profile.getMajor()).append("; ");
            if (profile.getGoal() != null) sb.append("目标:").append(profile.getGoal()).append("; ");
            if (profile.getNote() != null) sb.append("备注:").append(profile.getNote()).append("; ");
            sb.append("\n");
        }
        if (tasks != null && !tasks.isEmpty()) {
            sb.append("当前日程: ");
            String list = tasks.stream()
                    .map(t -> t.getTitle() + " [" + t.getPriority() + "] " +
                            (t.getStartTime() != null ? t.getStartTime() : "-") + " ~ " +
                            (t.getDeadline() != null ? t.getDeadline() : "-"))
                    .collect(Collectors.joining(" | "));
            sb.append(list);
        }
        sb.append("\n- 当用户请求创建/修改任务：返回以\"SET:\"开头的自然语言任务描述（中文），不要返回其它内容。");
        sb.append("\n- 当用户请求编写/生成 Java 工具，或你判断需要用代码回答时：返回以\"CODE:\"开头，紧跟文件名（如 Demo.java），提供完整 Java 源码，可用 ```java ... ``` 包裹，必须包含 main 方法。示例: CODE: Demo.java```java public class Demo { public static void main(String[] args){ System.out.println(\"hi\"); } }```");
        sb.append("\n- 若无需任务或代码，直接用中文简短回答。");
        return sb.toString();
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
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

    private String truncate(String text, int limit) {
        if (text == null) return "";
        return text.length() > limit ? text.substring(0, limit) + "..." : text;
    }
}
