package ru.ticketflowplatform.eventinventoryservice.util;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CollectionUtil {

    public static <T, R, C extends Collection<R>> C mapOrEmpty(
        Collection<T> source,
        Function<? super T, ? extends R> rule,
        Supplier<C> collectionFactory) {
        return source == null ? collectionFactory.get() : source.stream().map(rule).collect(Collectors.toCollection(collectionFactory));
    }
}
