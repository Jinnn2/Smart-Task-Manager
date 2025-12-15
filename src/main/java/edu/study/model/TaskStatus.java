package edu.study.model;

public enum TaskStatus {
    TODO,
    DOING,
    DONE,
    OVERDUE;

    public boolean isTerminal() {
        return this == DONE;
    }
}
