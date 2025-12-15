module edu.study {
    requires javafx.controls;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    requires io.github.cdimascio.dotenv.java;

    exports edu.study;
    exports edu.study.api;
    exports edu.study.controller;
    exports edu.study.model;
    exports edu.study.repository;
    exports edu.study.service;
    exports edu.study.ui;
    exports edu.study.util;

    opens edu.study.model to com.fasterxml.jackson.databind;
    opens edu.study.repository to com.fasterxml.jackson.databind;
}
