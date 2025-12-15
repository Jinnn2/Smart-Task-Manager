package edu.study.api.impl;

import edu.study.api.ChatClient;
import edu.study.model.PersonalProfile;
import edu.study.model.Task;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RuleChatClient implements ChatClient {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Override
    public Optional<String> chat(String userMessage, List<Task> tasks, PersonalProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("（本地规则回复）");
        if (profile != null && profile.getName() != null) {
            sb.append("你好，").append(profile.getName()).append("！ ");
        }
        sb.append("你刚才说：“").append(userMessage).append("”。 ");
        if (tasks != null && !tasks.isEmpty()) {
            String list = tasks.stream()
                    .sorted(Comparator.comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(t -> t.getTitle() + "(" + t.getPriority() + ") "
                            + formatTime(t.getStartTime()) + "~" + formatTime(t.getDeadline()))
                    .collect(Collectors.joining(" | "));
            sb.append("当前日程：").append(list);
        } else {
            sb.append("目前没有日程安排。");
        }
        return Optional.of(sb.toString());
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : DF.format(time);
    }
}
