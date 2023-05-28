package com.example.sipi.ui.queue;

public class Queue {
    String name;
    String status;
    String peoples;
    String slug;

    public Queue(String name, String status, String peoples, String slug) {
        this.name = name;
        switch (status) {
            case "true" -> this.status = "Открыта";
            case "false" -> this.status = "Закрыта";
        }
        this.peoples = peoples;
        this.slug = slug;
    }
}
