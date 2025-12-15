package edu.study.ui;

import edu.study.api.AssistantAPI;
import edu.study.api.ChatClient;
import edu.study.controller.TaskController;
import edu.study.model.PersonalProfile;
import edu.study.model.Priority;
import edu.study.model.SettingsConfig;
import edu.study.model.Task;
import edu.study.model.TaskStatus;
import edu.study.util.DateTimeUtil;
import edu.study.util.SettingsStore;
import edu.study.util.ToolSandboxRunner;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.io.PrintWriter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;

public class SmartTaskWidget {
    private final TaskController controller;
    private final AssistantAPI assistantAPI;
    private final ChatClient chatClient;
    private ListView<Task> unscheduledList;
    private HBox calendarBox;
    private Label statsLabel;
    private Label warningLabel;
    private Label nowTaskButtonLabel;
    private double dragOffsetX;
    private double dragOffsetY;
    private Stage ownerStage;
    private Task draggingTask;
    private LocalDate weekStart = LocalDate.now();
    private double pxPerHour = 24;
    private int refreshSeconds = 45;
    private Timeline refreshTimeline;
    private final double columnWidth = 150;
    private final double axisWidth = 60;
    private final double morningCompress = 0.25;// 0-8 点压缩比例
    private final double rightPanelWidth = 320;
    private final PersonalProfile profile = new PersonalProfile();
    private final ExecutorService llmExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService codeExecutor = Executors.newCachedThreadPool();
    private final ToolSandboxRunner sandboxRunner = new ToolSandboxRunner(Paths.get(System.getProperty("user.dir"), "Tools"));
    private VBox sandboxPane;
    private javafx.scene.control.ListView<String> jarListView;
    private javafx.scene.control.TextArea sandboxLog;

    public SmartTaskWidget(TaskController controller, AssistantAPI assistantAPI, ChatClient chatClient) {
        this.controller = controller;
        this.assistantAPI = assistantAPI;
        this.chatClient = chatClient;
    }

    public BorderPane build(Stage stage) {
        this.ownerStage = stage;
        loadSettings();
        unscheduledList = new ListView<>();
        unscheduledList.setCellFactory(lv -> new UnscheduledTaskCell());
        calendarBox = new HBox(8);
        calendarBox.setFillHeight(true);
        statsLabel = new Label();
        warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        nowTaskButtonLabel = new Label();

        BorderPane wrapper = new BorderPane();
        sandboxPane = new VBox();
        sandboxPane.setVisible(false);
        sandboxPane.setManaged(false);
        sandboxPane.setPrefWidth(320);
        sandboxPane.setMaxHeight(Double.MAX_VALUE);

        VBox mainContent = new VBox(10, buildHeader(stage), buildNowButton(), warningLabel, buildForm(), buildSplitPane(), buildAssistantBar(), buildChatPane());
        mainContent.setPadding(new Insets(10));
        VBox.setVgrow(calendarBox, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(unscheduledList, javafx.scene.layout.Priority.ALWAYS);
        double totalWidth = axisWidth + 7 * (columnWidth + 8) + rightPanelWidth + 60;
        double totalHeight = dayHeight() + 300;
        mainContent.setPrefWidth(totalWidth);
        mainContent.setPrefHeight(totalHeight);

        HBox container = new HBox(10, mainContent, sandboxPane);
        HBox.setHgrow(mainContent, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(sandboxPane, javafx.scene.layout.Priority.NEVER);
        wrapper.setCenter(container);
        wrapper.setBottom(buildFooter());

        refreshList();
        startAutoRefresh();
        enableDragging(stage, wrapper);
        return wrapper;
    }

    private Node buildHeader(Stage stage) {
        Label title = new Label("Smart Study Task Manager");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Button minimize = new Button("_");
        minimize.setOnAction(e -> stage.setIconified(true));
        minimize.setPrefWidth(30);
        Button settings = new Button("设置");
        settings.setOnAction(e -> showSettingsDialog());
        Button sandboxToggle = new Button("沙盒");
        sandboxToggle.setOnAction(e -> toggleSandbox());
        sandboxToggle.setTooltip(new Tooltip("显示/隐藏编程沙盒（Tools目录下的jar）"));

        Region spacer = new Region();
        HBox header = new HBox(10, title, spacer, sandboxToggle, settings, minimize);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node buildForm() {
        TextField titleField = new TextField();
        titleField.setPromptText("任务标题");

        TextField descriptionField = new TextField();
        descriptionField.setPromptText("描述");

        ComboBox<Priority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priorityBox.getSelectionModel().select(Priority.MEDIUM);
        priorityBox.setPrefWidth(120);

        DatePicker startPicker = new DatePicker(LocalDate.now());
        Spinner<Integer> startHour = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        startHour.setPrefWidth(70);

        DatePicker deadlinePicker = new DatePicker(LocalDate.now().plusDays(1));
        Spinner<Integer> deadlineHour = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23));
        deadlineHour.setPrefWidth(70);

        Spinner<Integer> durationSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 24, 2));
        durationSpinner.setPrefWidth(70);

        Button addBtn = new Button("添加");
        addBtn.setOnAction(e -> {
            String title = titleField.getText();
            if (title == null || title.isBlank()) {
                showAlert("请输入任务标题");
                return;
            }
            LocalDate deadlineDate = deadlinePicker.getValue();
            if (deadlineDate == null) {
                showAlert("请选择截止日期");
                return;
            }
            LocalDateTime startTime = null;
            if (startPicker.getValue() != null) {
                startTime = LocalDateTime.of(startPicker.getValue(), LocalTime.of(startHour.getValue(), 0));
            }
            LocalDateTime deadline = LocalDateTime.of(deadlineDate, LocalTime.of(deadlineHour.getValue(), 0));
            Duration estimated = Duration.ofHours(durationSpinner.getValue());
            controller.createTask(title, descriptionField.getText(), priorityBox.getValue(), deadline, estimated, null, startTime);
            titleField.clear();
            descriptionField.clear();
            refreshList();
        });

        HBox row1 = new HBox(8, titleField, addBtn);
        HBox row2 = new HBox(8, descriptionField, priorityBox);
        HBox row3 = new HBox(8, new Label("开始"), startPicker, startHour,
                new Label("截止"), deadlinePicker, deadlineHour, new Label("预估h"), durationSpinner);
        row1.setAlignment(Pos.CENTER_LEFT);
        row2.setAlignment(Pos.CENTER_LEFT);
        row3.setAlignment(Pos.CENTER_LEFT);
        return new VBox(5, row1, row2, row3);
    }

    private Node buildAssistantBar() {
        TextField input = new TextField();
        input.setPromptText("用自然语言快速记录 (例: 这周准备线代期中)");
        Button parseBtn = new Button("智能拆分");
        parseBtn.setOnAction(e -> {
            String text = input.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            llmExecutor.submit(() -> {
                assistantAPI.addTaskFromNaturalLanguage(text);
                javafx.application.Platform.runLater(() -> {
                    input.clear();
                    refreshList();
                });
            });
        });
        HBox box = new HBox(8, input, parseBtn);
        HBox.setHgrow(input, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private Node buildFooter() {
        VBox footer = new VBox(5);
        statsLabel.setText("加载中...");
        footer.getChildren().add(statsLabel);
        return footer;
    }

    private Node buildNowButton() {
        Button btn = new Button();
        btn.setOnAction(e -> {
            Task current = findCurrentTask();
            if (current != null) {
                controller.updateStatus(current.getTaskId(), TaskStatus.DOING);
                statsLabel.setText("已标记正在进行: " + current.getTitle());
            } else {
                showAlert("当前没有进行中的任务。");
            }
            refreshList();
        });
        btn.textProperty().bind(nowTaskButtonLabel.textProperty());
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void refreshList() {
        List<Task> unscheduled = controller.tasksWithoutSchedule();
        List<Task> scheduled = controller.tasksWithSchedule();
        unscheduledList.setItems(FXCollections.observableArrayList(unscheduled));
        updateCalendar(scheduled);
        long doneToday = controller.doneTodayCount();
        long highRisk = unscheduled.stream().filter(controller::isHighRisk).count()
                + scheduled.stream().filter(controller::isHighRisk).count();
        String base = "今日完成: " + doneToday + " | 高风险: " + highRisk
                + " | 未排期: " + unscheduled.size() + " | 已排期: " + scheduled.size();
        statsLabel.setText(base);
        updateCurrentButton(scheduled);
        updateWarnings(scheduled);
        updateUpcomingCard(scheduled, base);
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        refreshTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(refreshSeconds), e -> refreshList()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void enableDragging(Stage stage, BorderPane pane) {
        pane.setOnMousePressed(e -> {
            dragOffsetX = stage.getX() - e.getScreenX();
            dragOffsetY = stage.getY() - e.getScreenY();
        });
        pane.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragOffsetX);
            stage.setY(e.getScreenY() + dragOffsetY);
        });
    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private class UnscheduledTaskCell extends ListCell<Task> {
        private final Label title = new Label();
        private final Label deadline = new Label();
        private final ComboBox<TaskStatus> statusCombo = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));
        private final Button editBtn = new Button("Edit");
        private final Button deleteBtn = new Button("Del");

        UnscheduledTaskCell() {
            statusCombo.setOnAction(e -> {
                Task item = getItem();
                if (item != null && statusCombo.getValue() != null) {
                    controller.updateStatus(item.getTaskId(), statusCombo.getValue());
                    refreshList();
                }
            });
            editBtn.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    showEditDialog(item);
                }
            });
            deleteBtn.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    controller.removeTask(item.getTaskId());
                    refreshList();
                }
            });
            deleteBtn.setTooltip(new Tooltip("删除任务"));

            this.setOnDragDetected(this::onDragDetected);
            this.setOnDragDone(event -> {
                draggingTask = null;
                statsLabel.setText("拖放完成，列表已刷新");
                refreshList();
            });
        }

        private void onDragDetected(MouseEvent event) {
            Task item = getItem();
            if (item == null) {
                return;
            }
            draggingTask = item;
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(item.getTaskId().toString());
            db.setContent(content);
            db.setDragView(snapshot(new SnapshotParameters(), null));
            event.consume();
        }

        @Override
        protected void updateItem(Task item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            title.setText(item.getTitle() + " [" + item.getPriority() + "]");
            title.setStyle("-fx-font-weight: bold;");
            String deadlineText = item.getDeadline() == null ? "-" : DateTimeUtil.format(item.getDeadline());
            deadline.setText("截止: " + deadlineText + " • 预计: " + item.getEstimatedTime().toHours() + "h");
            statusCombo.getSelectionModel().select(item.getStatus());
            HBox actions = new HBox(6, statusCombo, editBtn, deleteBtn);
            Region spacer = new Region();
            HBox row = new HBox(10, new VBox(2, title, deadline), spacer, actions);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);

            boolean overdue = item.getStatus() == TaskStatus.OVERDUE;
            boolean highRisk = controller.isHighRisk(item);
            String bg = overdue ? "#ffe6e6" : highRisk ? "#fff6d9" : "white";
            row.setStyle("-fx-padding: 6; -fx-background-color: " + bg + "; -fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-background-radius: 4;");
            setGraphic(row);
            setText(null);
        }
    }

    private void showEditDialog(Task task) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("编辑任务");
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        TextField titleField = new TextField(task.getTitle());
        TextArea descField = new TextArea(task.getDescription());
        descField.setPrefRowCount(3);
        ComboBox<TaskStatus> statusBox = new ComboBox<>(FXCollections.observableArrayList(TaskStatus.values()));
        statusBox.getSelectionModel().select(task.getStatus());
        ComboBox<Priority> priorityBox = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priorityBox.getSelectionModel().select(task.getPriority());

        DatePicker startPicker = new DatePicker(task.getStartTime() != null ? task.getStartTime().toLocalDate() : LocalDate.now());
        Spinner<Integer> startHour = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23,
                task.getStartTime() != null ? task.getStartTime().getHour() : 8));

        DatePicker deadlinePicker = new DatePicker(task.getDeadline() != null ? task.getDeadline().toLocalDate() : LocalDate.now());
        Spinner<Integer> hourSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23,
                task.getDeadline() != null ? task.getDeadline().getHour() : 23));
        Spinner<Integer> estimateSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 48, (int) task.getEstimatedTime().toHours()));

        VBox body = new VBox(8,
                new HBox(8, new Label("标题"), titleField),
                new HBox(8, new Label("描述"), descField),
                new HBox(8, new Label("状态"), statusBox, new Label("优先级"), priorityBox),
                new HBox(8, new Label("开始"), startPicker, startHour),
                new HBox(8, new Label("截止"), deadlinePicker, hourSpinner, new Label("预估h"), estimateSpinner)
        );
        body.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                LocalDate date = deadlinePicker.getValue();
                LocalDateTime dl = date != null ? LocalDateTime.of(date, LocalTime.of(hourSpinner.getValue(), 0)) : null;
                LocalDate startDate = startPicker.getValue();
                LocalDateTime st = startDate != null ? LocalDateTime.of(startDate, LocalTime.of(startHour.getValue(), 0)) : null;
                controller.editTask(task.getTaskId(), titleField.getText(), descField.getText(), priorityBox.getValue(),
                        dl, Duration.ofHours(estimateSpinner.getValue()), null, st);
                controller.updateStatus(task.getTaskId(), statusBox.getValue());
                refreshList();
            }
            return null;
        });
        dialog.showAndWait();
    }

    private Node buildSplitPane() {
        Label calLabel = new Label("本周日程视图");
        ScrollPane calendarScroll = new ScrollPane(calendarBox);
        calendarScroll.setFitToWidth(false);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setPannable(true);
        calendarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        calendarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        double calWidth = axisWidth + 7 * (columnWidth + 8) + 20;
        calendarScroll.setPrefViewportHeight(dayHeight());
        calendarScroll.setPrefViewportWidth(calWidth);
        VBox calendarContainer = new VBox(6, calLabel, calendarScroll);
        calendarContainer.setPrefWidth(calWidth + 20);
        VBox.setVgrow(calendarContainer, javafx.scene.layout.Priority.ALWAYS);

        Label rightLabel = new Label("未排期任务（按重要性排序）");
        unscheduledList.setPrefWidth(300);
        VBox right = new VBox(6, rightLabel, unscheduledList);
        VBox.setVgrow(right, javafx.scene.layout.Priority.ALWAYS);

        SplitPane split = new SplitPane(calendarContainer, right);
        split.setDividerPositions(0.65);
        split.setPrefHeight(500);
        return split;
    }

    private void updateCalendar(List<Task> tasks) {
        calendarBox.getChildren().clear();
        double totalHeight = dayHeight();
        calendarBox.setSpacing(8);
        double totalWidth = axisWidth + 7 * (columnWidth + 8);
        calendarBox.setPrefWidth(totalWidth);
        calendarBox.setMinWidth(totalWidth);

        VBox axisBox = new VBox(4);
        Label axisHeader = new Label("时间");
        axisHeader.setStyle("-fx-font-weight: bold;");
        Pane axisPane = new Pane();
        axisPane.setPrefHeight(totalHeight);
        axisPane.setPrefWidth(axisWidth);
        int[] marks = {4, 8, 12, 16, 20};
        for (int h : marks) {
            Label l = new Label(String.format("%02d:00", h));
            l.setLayoutY(timeToY(LocalTime.of(h, 0)) - 6);
            axisPane.getChildren().add(l);
        }
        axisBox.getChildren().addAll(axisHeader, axisPane);
        calendarBox.getChildren().add(axisBox);

        LocalDate startDay = weekStart;
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDay.plusDays(i);
            VBox column = new VBox(6);
            column.setPrefWidth(columnWidth);
            column.setMinWidth(columnWidth);
            column.setPadding(new Insets(4));
            column.setStyle("-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-background-color: #fafafa;");
            Label header = new Label(day.getDayOfWeek() + "\n" + day);
            header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            List<Task> dayTasks = tasks.stream()
                    .filter(t -> t.getStartTime() != null && t.getStartTime().toLocalDate().isEqual(day))
                    .sorted(Comparator.comparing(Task::getStartTime))
                    .toList();

            Pane blocks = new Pane();
        blocks.setPrefHeight(totalHeight);
        blocks.setPrefWidth(columnWidth);
        blocks.setMinWidth(columnWidth);
        blocks.setStyle("-fx-background-color: #ffffff;");
        setupDropHandlers(blocks, day, totalHeight);
            for (Task t : dayTasks) {
                blocks.getChildren().add(renderBlock(t, pxPerHour));
            }
            addNowLine(blocks, day);
            column.getChildren().addAll(header, blocks);
            VBox.setVgrow(blocks, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(column, javafx.scene.layout.Priority.NEVER);
            calendarBox.getChildren().add(column);
        }
    }

    private Node renderBlock(Task task, double pxPerHour) {
        String color = priorityColor(task.getPriority());
        Label block = new Label(task.getTitle() + "\n" + timeRange(task));
        block.setWrapText(true);
        block.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6; -fx-padding: 6; -fx-text-fill: #0f0f0f; -fx-font-size: 11px;");
        block.setMinHeight(40);
        double blockWidth = columnWidth - 12;
        block.setPrefWidth(blockWidth);
        block.setMinWidth(blockWidth);
        block.setMaxWidth(blockWidth);

        LocalDateTime start = task.getStartTime();
        Duration duration = task.getEstimatedTime() != null ? task.getEstimatedTime() : Duration.ofHours(1);
        double height = durationHeight(start, duration);
        double offset = start != null ? timeToY(start.toLocalTime()) : 0;
        block.setPrefHeight(height);
        block.setLayoutY(offset);
        block.setMaxWidth(blockWidth);
        if (start != null) {
            LocalDateTime end = start.plus(duration);
            LocalDateTime now = LocalDateTime.now();
            if (!now.isBefore(start) && now.isBefore(end)) {
                block.setStyle(block.getStyle() + "-fx-border-color: #2e6cf6; -fx-border-width: 2;");
            } else if (now.isAfter(end)) {
                block.setStyle(block.getStyle() + "-fx-opacity: 0.5;");
            }
            if (end.isAfter(task.getDeadline() != null ? task.getDeadline() : end)) {
                block.setStyle(block.getStyle() + "-fx-border-color: red;");
            }
        }
        if (task.getPostponeCount() > 0) {
            block.setText("! " + block.getText());
            block.setStyle(block.getStyle() + "-fx-text-fill: #b30000;");
        }
        block.setOnMouseClicked(e -> {
            if (e.getButton().name().equals("PRIMARY")) {
                showEditDialog(task);
            }
        });
        block.setOnDragDetected(e -> {
            if (!e.isSecondaryButtonDown()) return;
            draggingTask = task;
            Dragboard db = block.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(task.getTaskId().toString());
            db.setContent(content);
            db.setDragView(block.snapshot(new SnapshotParameters(), null));
            e.consume();
        });
        return block;
    }

    private String timeRange(Task task) {
        LocalDateTime start = task.getStartTime();
        if (start == null) {
            return "未设置时间";
        }
        Duration duration = safeDuration(task);
        LocalDateTime end = start.plus(duration);
        return start.toLocalTime().truncatedTo(ChronoUnit.MINUTES) + " - " + end.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private String priorityColor(Priority priority) {
        Map<Priority, String> map = new EnumMap<>(Priority.class);
        map.put(Priority.CRITICAL, "#ff9aa2");
        map.put(Priority.HIGH, "#ffdac1");
        map.put(Priority.MEDIUM, "#e2f0cb");
        map.put(Priority.LOW, "#c7ceea");
        return map.getOrDefault(priority, "#e2f0cb");
    }

    private void setupDropHandlers(Pane pane, LocalDate day, double totalHeight) {
        pane.setOnDragOver(event -> {
            if (draggingTask != null && event.getGestureSource() != pane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
                double y = clamp(event.getY(), 0, totalHeight);
                LocalTime snapped = snapToSlot(y);
                statsLabel.setText("拖放到 " + day + " " + snapped);
            }
            event.consume();
        });
        pane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Task task = draggingTask != null ? draggingTask : findTaskById(db.getString());
                if (task != null) {
                    double y = clamp(event.getY(), 0, totalHeight);
                    LocalTime snapped = snapToSlot(y);
                    LocalDateTime start = LocalDateTime.of(day, snapped);
                    LocalDateTime oldStart = task.getStartTime();
                    boolean delayed = oldStart != null && start.isAfter(oldStart);
                    if (delayed) {
                        task.incrementPostponeCount();
                    }
                    controller.editTask(task.getTaskId(), null, null, null, task.getDeadline(), task.getEstimatedTime(), task.getCourseId(), start);
                    success = true;
                    refreshList();
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private LocalTime snapToSlot(double y) {
        return yToTime(y);
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private Task findTaskById(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            for (Task t : unscheduledList.getItems()) {
                if (uuid.equals(t.getTaskId())) {
                    return t;
                }
            }
            for (Task t : controller.tasksWithSchedule()) {
                if (uuid.equals(t.getTaskId())) {
                    return t;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private double compressedMorningHeight() {
        return 8 * pxPerHour * morningCompress;
    }

    private double dayHeight() {
        return compressedMorningHeight() + 16 * pxPerHour;
    }

    private double timeToY(LocalTime time) {
        double hour = time.getHour() + time.getMinute() / 60.0;
        if (hour <= 8) {
            return hour * pxPerHour * morningCompress;
        }
        return compressedMorningHeight() + (hour - 8) * pxPerHour;
    }

    private LocalTime yToTime(double y) {
        double morningH = compressedMorningHeight();
        double hours;
        if (y <= morningH) {
            hours = y / (pxPerHour * morningCompress);
        } else {
            hours = 8 + (y - morningH) / pxPerHour;
        }
        hours = Math.max(0, Math.min(23.99, hours));
        int halfSlots = (int) Math.round(hours * 2);
        int totalMinutes = Math.min(23 * 60 + 30, halfSlots * 30);
        return LocalTime.of(totalMinutes / 60, totalMinutes % 60);
    }

    private double durationHeight(LocalDateTime start, Duration duration) {
        if (start == null) {
            return 40;
        }
        LocalDateTime end = start.plus(duration != null ? duration : Duration.ofHours(1));
        if (!end.toLocalDate().isEqual(start.toLocalDate())) {
            end = start.toLocalDate().atTime(23, 59);
        }
        double h = timeToY(end.toLocalTime()) - timeToY(start.toLocalTime());
        return Math.max(40, h);
    }

    private void addNowLine(Pane pane, LocalDate day) {
        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalDate().isEqual(day)) {
            return;
        }
        double y = timeToY(now.toLocalTime());
        Region line = new Region();
        line.setStyle("-fx-background-color: #2e6cf6;");
        line.setPrefHeight(2);
        line.setPrefWidth(Double.MAX_VALUE);
        line.setLayoutY(y);
        pane.getChildren().add(line);
    }

    private Task findCurrentTask() {
        LocalDateTime now = LocalDateTime.now();
        for (Task t : controller.tasksWithSchedule()) {
            LocalDateTime start = t.getStartTime();
            Duration duration = safeDuration(t);
            if (start != null && !now.isBefore(start) && now.isBefore(start.plus(duration))) {
                return t;
            }
        }
        return null;
    }

    private void updateCurrentButton(List<Task> scheduled) {
        Task current = findCurrentTask();
        if (current != null) {
            nowTaskButtonLabel.setText("你在完成【" + current.getTitle() + "】吗？");
        } else {
            nowTaskButtonLabel.setText("当前没有进行中的任务，点击提示为空");
        }
    }

    private void updateWarnings(List<Task> scheduled) {
        boolean hasOverrun = scheduled.stream().anyMatch(t -> {
            if (t.getStartTime() == null || t.getDeadline() == null || t.getEstimatedTime() == null) return false;
            return t.getStartTime().plus(safeDuration(t)).isAfter(t.getDeadline());
        });
        warningLabel.setText(hasOverrun ? "警报：存在任务预计结束时间超过截止时间！" : "");
    }

    private void updateUpcomingCard(List<Task> scheduled, String base) {
        LocalDateTime now = LocalDateTime.now();
        Task next = scheduled.stream()
                .filter(t -> t.getStartTime() != null && t.getStartTime().isAfter(now))
                .min(Comparator.comparing(Task::getStartTime))
                .orElse(null);
        if (next == null) {
            statsLabel.setText(base + " | 下一任务：无");
            return;
        }
        Duration until = Duration.between(now, next.getStartTime());
        String info = String.format("下一项：%s (优先级:%s) 距开始 %d分", next.getTitle(), next.getPriority(), until.toMinutes());
        if (next.getStartTime().plus(safeDuration(next)).isBefore(now)) {
            info += " [已过期]";
        }
        statsLabel.setText(base + " | " + info);
    }

    private Duration safeDuration(Task task) {
        return task.getEstimatedTime() != null ? task.getEstimatedTime() : Duration.ofHours(1);
    }

    private Node buildChatPane() {
        Label chatLabel = new Label("与个人助手聊天");
        TextArea chatHistory = new TextArea();
        chatHistory.setEditable(false);
        chatHistory.setWrapText(true);
        chatHistory.setPrefHeight(150);

        TextField chatInput = new TextField();
        chatInput.setPromptText("输入想聊的内容，例如：帮我规划明天的学习安排");
        Button send = new Button("发送");
        send.setOnAction(e -> {
            String msg = chatInput.getText();
            if (msg == null || msg.isBlank()) {
                return;
            }
            chatHistory.appendText("你：" + msg + "\n");
            chatInput.clear();
            List<Task> all = controller.listTasks();
            llmExecutor.submit(() -> {
                chatClient.chat(msg, all, profile).ifPresentOrElse(
                        r -> javafx.application.Platform.runLater(() -> {
                            if (r.startsWith("SET:")) {
                                String nl = r.substring(4).trim();
                                assistantAPI.addTaskFromNaturalLanguage(nl);
                                refreshList();
                                chatHistory.appendText("助手: 已根据指令创建/更新任务。\n");
                            } else if (r.trim().toUpperCase().contains("CODE:") || r.contains("```")) {
                                chatHistory.appendText("助手: 收到代码生成请求，正在构建...\n");
                                appendSandboxLog("[code] 收到回复，准备处理。长度=" + r.length());
                                try {
                                    appendSandboxLog("[code] before handleCodeResponse (同步执行调试)");
                                    handleCodeResponse(r, chatHistory);
                                    appendSandboxLog("[code] handleCodeResponse end");
                                } catch (Throwable ex) {
                                    appendSandboxLog("[code] handleCodeResponse 异常: " + ex.getMessage());
                                    appendSandboxLog(getStackTrace(ex));
                                }
                            } else {
                                chatHistory.appendText("助手: " + r + "\n");
                            }
                        }),
                        () -> javafx.application.Platform.runLater(() -> chatHistory.appendText("助手: 未找到相关任务或指令。\n"))
                );
            });
        });
        HBox inputRow = new HBox(8, chatInput, send);
        HBox.setHgrow(chatInput, javafx.scene.layout.Priority.ALWAYS);
        return new VBox(6, chatLabel, chatHistory, inputRow);
    }

    private void toggleSandbox() {
        if (sandboxPane.getChildren().isEmpty()) {
            sandboxPane.getChildren().setAll(buildSandboxPane());
        }
        boolean visible = !sandboxPane.isVisible();
        sandboxPane.setVisible(visible);
        sandboxPane.setManaged(visible);
        if (!visible) {
            sandboxPane.setPrefWidth(0);
        } else {
            sandboxPane.setPrefWidth(320);
        }
        if (visible) {
            refreshSandboxList();
        }
    }

    private Node buildSandboxPane() {
        Label title = new Label("编程沙盒（Tools目录下的jar）");
        title.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        jarListView = new ListView<>();
        jarListView.setStyle("-fx-control-inner-background: #000000; -fx-text-fill: #e0e0e0;");
        jarListView.setPrefHeight(160);

        Button refresh = new Button("刷新");
        refresh.setOnAction(e -> refreshSandboxList());
        Button run = new Button("运行所选");
        run.setOnAction(e -> {
            String jar = jarListView.getSelectionModel().getSelectedItem();
            if (jar == null || jar.isBlank()) {
                appendSandboxLog("[warn] 未选择程序");
                return;
            }
            appendSandboxLog("[run] java -jar " + jar);
            sandboxRunner.runJar(jar, line -> javafx.application.Platform.runLater(() -> appendSandboxLog(line)));
        });
        HBox actions = new HBox(8, refresh, run);

        sandboxLog = new TextArea();
        sandboxLog.setEditable(false);
        sandboxLog.setWrapText(true);
        sandboxLog.setPrefHeight(140);
        sandboxLog.setStyle("-fx-control-inner-background: #000000; -fx-text-fill: #00ff66; -fx-font-family: Consolas, monospace;");
        VBox.setVgrow(sandboxLog, javafx.scene.layout.Priority.ALWAYS);

        VBox box = new VBox(8, title, jarListView, actions, sandboxLog);
        box.setStyle("-fx-background-color: #000000; -fx-padding: 8;");
        return box;
    }

    private void refreshSandboxList() {
        List<String> jars = sandboxRunner.listJars();
        jarListView.getItems().setAll(jars);
        appendSandboxLog("[info] 已加载 " + jars.size() + " 个jar");
    }

    private void appendSandboxLog(String line) {
        System.err.println("[sandbox] " + line);
        logToFile(line);
        Platform.runLater(() -> {
            if (sandboxLog != null) {
                sandboxLog.appendText(line + "\n");
            }
        });
    }

    private void handleCodeResponse(String response, TextArea chatHistory) {
        appendSandboxLog("\nIN\n");
        try {
            appendSandboxLog("[code] handleCodeResponse start");
            System.err.println("[sandbox] handleCodeResponse start (thread=" + Thread.currentThread().getName() + ")");
            logToFile("[code-file] entered handleCodeResponse thread=" + Thread.currentThread().getName());
            int idx = response.toUpperCase().indexOf("CODE:");
            String body = idx >= 0 ? response.substring(idx + "CODE:".length()).trim() : response.trim();
            appendSandboxLog("[code] body preview: " + body.substring(0, Math.min(80, body.length())).replaceAll("\\s+", " "));
            String code = extractCode(body);
            if (code == null || code.isBlank()) {
                appendSandboxLog("[code] code parse failed/empty");
                javafx.application.Platform.runLater(() -> chatHistory.appendText("助手: 代码内容为空，已忽略。\n"));
                return;
            }
            String fileName = extractFileName(body);
            String className = extractClassName(code);
            if (className == null) {
                className = "GeneratedTool" + System.currentTimeMillis();
                appendSandboxLog("[code] className fallback: " + className);
            }
            if (fileName == null || fileName.isBlank()) {
                fileName = className + ".java";
                appendSandboxLog("[code] fileName fallback: " + fileName);
            }
            final String finalClassName = className;
            final String finalFileName = fileName;
            Path toolsDir = Paths.get(System.getProperty("user.dir"), "Tools");
            Files.createDirectories(toolsDir);
            Path javaFile = toolsDir.resolve(fileName);
            Files.writeString(javaFile, code, StandardCharsets.UTF_8);
            appendSandboxLog("[code] 写入源码: " + javaFile.getFileName());

            StringBuilder log = new StringBuilder();
            int compileExit = runProcess(log, toolsDir, resolveTool("javac"), "-encoding", "UTF-8", javaFile.getFileName().toString());
            if (compileExit != 0) {
                Path corrupt = toolsDir.resolve(finalClassName + ".corrupt");
                Files.writeString(corrupt, "Compile failed:\n" + log, StandardCharsets.UTF_8);
                final Path corruptPath = corrupt;
                final String logText = log.toString();
                javafx.application.Platform.runLater(() -> {
                    appendSandboxLog("[code] 编译失败，已标记为损坏: " + corruptPath.getFileName());
                    appendSandboxLog(logText);
                    chatHistory.appendText("助手: 代码编译失败，已标记损坏。\n");
                });
                return;
            }
            appendSandboxLog("[code] 编译成功: " + javaFile.getFileName());
            int jarExit = runProcess(log, toolsDir, resolveTool("jar"), "cfe", finalClassName + ".jar", finalClassName, finalClassName + ".class");
            if (jarExit != 0) {
                Path corrupt = toolsDir.resolve(finalClassName + ".corrupt");
                Files.writeString(corrupt, "Jar failed:\n" + log, StandardCharsets.UTF_8);
                final Path corruptPath = corrupt;
                final String logText = log.toString();
                javafx.application.Platform.runLater(() -> {
                    appendSandboxLog("[code] 打包失败，已标记为损坏: " + corruptPath.getFileName());
                    appendSandboxLog(logText);
                    chatHistory.appendText("助手: 代码打包失败，已标记损坏。\n");
                });
                return;
            }
            final String logText = log.toString();
            javafx.application.Platform.runLater(() -> {
                appendSandboxLog("[code] 生成完成: " + finalClassName + ".jar");
                if (!logText.isEmpty()) {
                    appendSandboxLog(logText);
                }
                chatHistory.appendText("助手: 代码已生成并打包，可在沙盒列表中运行 " + finalClassName + ".jar\n");
                refreshSandboxList();
            });
        } catch (Throwable ex) {
            javafx.application.Platform.runLater(() -> {
                appendSandboxLog("[code] 异常: " + ex.getMessage());
                chatHistory.appendText("助手: 处理代码时出现异常。\n");
            });
            appendSandboxLog("[code] 异常堆栈:\n" + getStackTrace(ex));
        }
    }

    private String extractCode(String body) {
        if (body.contains("```")) {
            int first = body.indexOf("```");
            int last = body.lastIndexOf("```");
            if (last > first + 3) {
                String inside = body.substring(first + 3, last).trim();
                if (inside.startsWith("java")) {
                    inside = inside.substring(4).trim();
                }
                return inside;
            }
        }
        return body;
    }

    private String resolveTool(String tool) {
        Path javaHome = Paths.get(System.getProperty("java.home"));
        Path candidate = javaHome.resolve("bin").resolve(tool + (isWindows() ? ".exe" : ""));
        if (Files.exists(candidate)) {
            return candidate.toString();
        }
        return tool;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String extractFileName(String body) {
        String firstLine = body.split("\\R", 2)[0].trim();
        if (firstLine.toLowerCase().endsWith(".java")) {
            return firstLine;
        }
        return null;
    }

    private String extractClassName(String code) {
        Pattern p = Pattern.compile("class\\s+(\\w+)");
        Matcher m = p.matcher(code);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private int runProcess(StringBuilder log, Path dir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.append(line).append("\n");
            }
        }
        return p.waitFor();
    }

    private String getStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private void logToFile(String msg) {
        try {
            Path logDir = Paths.get(System.getProperty("user.dir"), "logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("code-debug.log");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND))) {
                pw.println(java.time.LocalDateTime.now() + " " + msg);
            }
        } catch (IOException ignored) {
        }
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("设置");
        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

                Spinner<Integer> pxSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 80, (int) pxPerHour));
        Spinner<Integer> refreshSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 300, refreshSeconds));
        TextField nameField = new TextField(profile.getName());
        TextField majorField = new TextField(profile.getMajor());
        TextField goalField = new TextField(profile.getGoal());
        TextArea noteArea = new TextArea(profile.getNote());
        noteArea.setPrefRowCount(3);
        Button resetBtn = new Button("清空数据");
        resetBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确认清空所有数据并重置？", ButtonType.OK, ButtonType.CANCEL);
            confirm.initOwner(dialog.getDialogPane().getScene().getWindow());
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    controller.resetAll();
                    refreshList();
                }
            });
        });

        VBox body = new VBox(10,
                new HBox(8, new Label("每小时像素"), pxSpinner),
                new HBox(8, new Label("刷新间隔(秒)"), refreshSpinner),
                new HBox(8, new Label("姓名"), nameField, new Label("专业"), majorField),
                new HBox(8, new Label("目标"), goalField),
                new VBox(4, new Label("备注"), noteArea),
                new Separator(),
                resetBtn
        );
        body.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                pxPerHour = pxSpinner.getValue();
                refreshSeconds = refreshSpinner.getValue();
                profile.setName(nameField.getText());
                profile.setMajor(majorField.getText());
                profile.setGoal(goalField.getText());
                profile.setNote(noteArea.getText());
                startAutoRefresh();
                refreshList();
                saveSettings();
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void loadSettings() {
        SettingsConfig cfg = SettingsStore.load();
        if (cfg != null) {
            if (cfg.getPxPerHour() > 0) {
                pxPerHour = cfg.getPxPerHour();
            }
            if (cfg.getRefreshSeconds() > 0) {
                refreshSeconds = cfg.getRefreshSeconds();
            }
            PersonalProfile p = cfg.getProfile();
            if (p != null) {
                profile.setName(p.getName());
                profile.setMajor(p.getMajor());
                profile.setGoal(p.getGoal());
                profile.setNote(p.getNote());
            }
        }
    }

    private void saveSettings() {
        SettingsConfig cfg = new SettingsConfig();
        cfg.setPxPerHour(pxPerHour);
        cfg.setRefreshSeconds(refreshSeconds);
        PersonalProfile copy = new PersonalProfile();
        copy.setName(profile.getName());
        copy.setMajor(profile.getMajor());
        copy.setGoal(profile.getGoal());
        copy.setNote(profile.getNote());
        cfg.setProfile(copy);
        SettingsStore.save(cfg);
    }

    public void shutdown() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        llmExecutor.shutdownNow();
        codeExecutor.shutdownNow();
        saveSettings();
    }
}
