package edu.study.api;

import edu.study.model.PersonalProfile;
import edu.study.model.Task;
import java.util.List;
import java.util.Optional;

public interface ChatClient {
    Optional<String> chat(String userMessage, List<Task> tasks, PersonalProfile profile);
}
