package com.valamis.learning.taskreminder.service;

import com.valamis.learning.taskreminder.domain.Task;
import java.util.List;

public record DailyTasks(List<Task> openTasks, List<Task> holdTasks) {

    public DailyTasks {
        openTasks = List.copyOf(openTasks);
        holdTasks = List.copyOf(holdTasks);
    }
}
