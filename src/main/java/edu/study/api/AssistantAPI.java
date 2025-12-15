package edu.study.api;

public interface AssistantAPI {
    String queryTaskSummary();

    String queryTodayPlan();

    void addTaskFromNaturalLanguage(String input);
}
