package com.valamis.learning.taskreminder.persistence;

import com.valamis.learning.taskreminder.domain.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(long id);

    List<Task> findAll();

    void deleteById(long id);

    void deleteAll();

    List<Task> saveAll(List<Task> tasks);

    List<Task> replaceAll(List<Task> tasks);
}
