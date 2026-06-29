package com.valamis.learning.taskreminder.persistence;

import com.valamis.learning.taskreminder.domain.Task;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link TaskRepository} used across the test suites. Mirrors the
 * {@link SqliteTaskRepository} contract (auto-incrementing ids, updated-date
 * ordering, batch insert and replace) without touching a database.
 */
public final class InMemoryTaskRepository implements TaskRepository {

    private final List<Task> tasks = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public Task save(Task task) {
        Task saved = task.id() == null ? task.withId(nextId++) : task;
        deleteById(saved.id());
        tasks.add(saved);
        return saved;
    }

    @Override
    public Optional<Task> findById(long id) {
        return tasks.stream().filter(task -> task.id() == id).findFirst();
    }

    @Override
    public List<Task> findAll() {
        return tasks.stream()
                .sorted(Comparator.comparing(Task::updatedDate).reversed().thenComparing(Task::taskName))
                .toList();
    }

    @Override
    public void deleteById(long id) {
        tasks.removeIf(task -> task.id() == id);
    }

    @Override
    public void deleteAll() {
        tasks.clear();
    }

    @Override
    public List<Task> saveAll(List<Task> tasksToSave) {
        return tasksToSave.stream().map(this::save).toList();
    }

    @Override
    public List<Task> replaceAll(List<Task> replacementTasks) {
        tasks.clear();
        return saveAll(replacementTasks);
    }
}
