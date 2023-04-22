package zad1;

import java.io.Serializable;

public enum EventType {
    LOGIN("login"),
    MESSAGE("message"),
    LOGOUT("logout");

    EventType(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }

    final String name;
}
