package edu.study;

import edu.study.api.AssistantAPI;
import edu.study.api.ChatClient;
import edu.study.api.impl.OpenAIChatAssistantAPI;
import edu.study.api.impl.OpenAIChatClient;
import edu.study.api.impl.RuleBasedAssistantAPI;
import edu.study.api.impl.RuleChatClient;
import edu.study.controller.TaskController;
import edu.study.repository.FileTaskRepository;
import edu.study.repository.JsonDataRepository;
import edu.study.repository.TaskRepository;
import edu.study.service.AnalyticsService;
import edu.study.service.SchedulingService;
import edu.study.service.TaskService;
import edu.study.ui.SmartTaskWidget;
import edu.study.util.FileUtil;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Path;
import java.util.Optional;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Path storagePath = FileUtil.defaultStoragePath();
        FileUtil.ensureFile(storagePath);
        JsonDataRepository jsonRepo = new JsonDataRepository(storagePath);
        TaskRepository taskRepository = new FileTaskRepository(jsonRepo);
        TaskService taskService = new TaskService(taskRepository, new SchedulingService());
        AnalyticsService analyticsService = new AnalyticsService();
        TaskController taskController = new TaskController(taskService, analyticsService);
        AssistantAPI assistantAPI = buildAssistant(taskService);
        ChatClient chatClient = buildChatClient();

        SmartTaskWidget widget = new SmartTaskWidget(taskController, assistantAPI, chatClient);
        Scene scene = new Scene(widget.build(stage));
        stage.setTitle("Smart Study Task Manager");
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
        stage.setOnCloseRequest(event -> {
            widget.shutdown();
            taskController.shutdown();
        });
    }

    public static void main(String[] args) {
        launch();
    }

    private AssistantAPI buildAssistant(TaskService taskService) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = firstNonBlank(dotenv.get("OPENAI_API_KEY"), System.getenv("OPENAI_API_KEY"));
        if (apiKey != null && !apiKey.isBlank()) {
            String baseUrl = Optional.ofNullable(firstNonBlank(dotenv.get("OPENAI_BASE_URL"), System.getenv("OPENAI_BASE_URL")))
                    .filter(s -> !s.isBlank())
                    .orElse("https://api.openai.com/v1");
            String model = Optional.ofNullable(firstNonBlank(dotenv.get("OPENAI_MODEL"), System.getenv("OPENAI_MODEL")))
                    .filter(s -> !s.isBlank())
                    .orElse("gpt-3.5-turbo");
            return new OpenAIChatAssistantAPI(taskService, apiKey, baseUrl, model);
        }
        return new RuleBasedAssistantAPI(taskService);
    }

    private ChatClient buildChatClient() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = firstNonBlank(dotenv.get("OPENAI_API_KEY"), System.getenv("OPENAI_API_KEY"));
        if (apiKey != null && !apiKey.isBlank()) {
            String baseUrl = Optional.ofNullable(firstNonBlank(dotenv.get("OPENAI_BASE_URL"), System.getenv("OPENAI_BASE_URL")))
                    .filter(s -> !s.isBlank())
                    .orElse("https://api.openai.com/v1");
            String model = Optional.ofNullable(firstNonBlank(dotenv.get("OPENAI_MODEL"), System.getenv("OPENAI_MODEL")))
                    .filter(s -> !s.isBlank())
                    .orElse("gpt-3.5-turbo");
            return new OpenAIChatClient(apiKey, baseUrl, model);
        }
        return new RuleChatClient();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
