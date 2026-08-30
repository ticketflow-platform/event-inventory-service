package ru.ticketflowplatform.eventinventoryservice;

import org.springframework.boot.SpringApplication;

public class TestEventInventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(EventInventoryServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
