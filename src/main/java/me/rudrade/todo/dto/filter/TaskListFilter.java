package me.rudrade.todo.dto.filter;

import me.rudrade.todo.model.User;

public record TaskListFilter(Filter filter, String searchTerm, User user) {

    public enum Filter {
        UPCOMING,
        TODAY,
        SEARCH,
        LIST,
        TAG
    }
}

