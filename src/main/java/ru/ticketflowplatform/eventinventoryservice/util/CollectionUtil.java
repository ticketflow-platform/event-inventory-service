package ru.ticketflowplatform.eventinventoryservice.util;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;
import lombok.Generated;

public final class CollectionUtil {

    @Generated
    private CollectionUtil() {
    }

    public static <T, A, R> R collectOrEmpty(
        Collection<T> source,
        Collector<? super T, A, R> collector) {
        return (source == null ? Stream.<T>empty() : source.stream()).collect(collector);
    }

    public static <T, A, R> R filterDistinctOrEmpty(
        Collection<T> source,
        Predicate<? super T> rule,
        Collector<? super T, A, R> collector) {
        return (source == null ? Stream.<T>empty() : source.stream())
            .filter(rule)
            .distinct()
            .collect(collector);
    }
}
