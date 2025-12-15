package edu.study.repository;

import edu.study.model.Task;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    List<Task> findAll();

    Optional<Task> findById(UUID id);

    Task save(Task task);

    void saveAll(List<Task> tasks);

    boolean delete(UUID id);

    void refresh();

    void persist();
}
