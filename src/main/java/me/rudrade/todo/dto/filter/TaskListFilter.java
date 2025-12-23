package me.rudrade.todo.dto.filter;

public record TaskListFilter(Filter filter, String searchTerm) {

    public enum Filter {
        UPCOMING,
        TODAY,
        SEARCH,
        LIST,
        TAG
    }
}

