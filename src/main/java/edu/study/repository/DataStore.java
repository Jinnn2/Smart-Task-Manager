package edu.study.repository;

import edu.study.model.Course;
import edu.study.model.Task;
import edu.study.model.TimeBlock;
import edu.study.model.User;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private List<Task> tasks = new ArrayList<>();
    private List<Course> courses = new ArrayList<>();
    private List<TimeBlock> timeBlocks = new ArrayList<>();
    private List<User> users = new ArrayList<>();

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public List<TimeBlock> getTimeBlocks() {
        return timeBlocks;
    }

    public void setTimeBlocks(List<TimeBlock> timeBlocks) {
        this.timeBlocks = timeBlocks;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
