package ru.ticketflowplatform.eventinventoryservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class EventInventoryServiceApplicationTest {

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenario.cases.java.EventInventoryServiceApplicationTestCases#main_whenArgumentsAreProvided_thenLaunchSpringApplicationTestCases")
    @DisplayName("Передаёт аргументы в запуск Spring Boot")
    void main_whenArgumentsAreProvided_thenLaunchSpringApplicationTest(
        String scenario, String[] args) {
            try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
                EventInventoryServiceApplication.main(args);

                springApplication.verify(
                    () -> SpringApplication.run(EventInventoryServiceApplication.class, args));
            }
    }
}
