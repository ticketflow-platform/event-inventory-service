package ru.ticketflowplatform.eventinventoryservice.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import ru.ticketflowplatform.eventinventoryservice.domain.model.consts.I18N;

@Configuration
public class LocaleConfig {

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag(I18N.RUSSIAN_LANGUAGE_TAG));
        resolver.setSupportedLocales(List.of(Locale.forLanguageTag(I18N.RUSSIAN_LANGUAGE_TAG), Locale.ENGLISH));
        return resolver;
    }
}
