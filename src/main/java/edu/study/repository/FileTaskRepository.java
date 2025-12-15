package edu.study.repository;

import edu.study.model.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileTaskRepository implements TaskRepository {
    private final JsonDataRepository jsonRepository;
    private DataStore cache;

    public FileTaskRepository(JsonDataRepository jsonRepository) {
        this.jsonRepository = jsonRepository;
        this.cache = ensureLists(jsonRepository.load());
    }

    private DataStore ensureLists(DataStore dataStore) {
        if (dataStore.getTasks() == null) {
            dataStore.setTasks(new ArrayList<>());
        }
        if (dataStore.getCourses() == null) {
            dataStore.setCourses(new ArrayList<>());
        }
        if (dataStore.getTimeBlocks() == null) {
            dataStore.setTimeBlocks(new ArrayList<>());
        }
        if (dataStore.getUsers() == null) {
            dataStore.setUsers(new ArrayList<>());
        }
        return dataStore;
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(cache.getTasks());
    }

    @Override
    public synchronized Optional<Task> findById(UUID id) {
        return cache.getTasks().stream()
                .filter(t -> t.getTaskId() != null && t.getTaskId().equals(id))
                .findFirst();
    }

    @Override
    public synchronized Task save(Task task) {
        if (task.getTaskId() == null) {
            task.setTaskId(UUID.randomUUID());
        }
        cache.getTasks().removeIf(t -> task.getTaskId().equals(t.getTaskId()));
        cache.getTasks().add(task);
        persist();
        return task;
    }

    @Override
    public synchronized void saveAll(List<Task> tasks) {
        cache.setTasks(new ArrayList<>(tasks));
        persist();
    }

    @Override
    public synchronized boolean delete(UUID id) {
        boolean removed = cache.getTasks().removeIf(t -> id.equals(t.getTaskId()));
        if (removed) {
            persist();
        }
        return removed;
    }

    @Override
    public synchronized void refresh() {
        cache = ensureLists(jsonRepository.load());
    }

    @Override
    public synchronized void persist() {
        jsonRepository.save(cache);
    }
}
