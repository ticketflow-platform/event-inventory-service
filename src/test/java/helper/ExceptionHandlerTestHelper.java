package helper;

import java.util.List;
import java.util.Locale;
import org.mockito.Mockito;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.ticketflowplatform.eventinventoryservice.controller.EventController;
import ru.ticketflowplatform.eventinventoryservice.controller.GlobalExceptionHandler;
import ru.ticketflowplatform.eventinventoryservice.mapper.ErrorMapperImpl;
import ru.ticketflowplatform.eventinventoryservice.service.EventService;

public final class ExceptionHandlerTestHelper {

    private ExceptionHandlerTestHelper() {
    }

    public static HandlerExceptionResolver globalResolver() {
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton(
            "globalExceptionHandler", new GlobalExceptionHandler(new ErrorMapperImpl(), TestMessageSources.i18n())
        );
        ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
        resolver.setApplicationContext(context);
        resolver.setMessageConverters(List.of(new JacksonJsonHttpMessageConverter()));
        resolver.afterPropertiesSet();
        return resolver;
    }

    public static MockHttpServletRequest request(Locale locale) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setPreferredLocales(List.of(locale));
        request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    public static MethodParameter eventParameter(int index) {
        RequestMappingHandlerMapping mapping = MockMvcBuilders
            .standaloneSetup(new EventController(Mockito.mock(EventService.class)))
            .addPlaceholderValue("api.urls.v1.event", "/api/v1/events")
            .build().getDispatcherServlet().getWebApplicationContext()
            .getBean(RequestMappingHandlerMapping.class);
        return mapping.getHandlerMethods().values().stream()
            .filter(handler -> handler.getBeanType().equals(EventController.class))
            .findFirst().orElseThrow().getMethodParameters()[index];
    }

    public static HandlerMethodValidationException methodValidation(
        String headerName,
        String queryName,
        String parameterName,
        List<? extends MessageSourceResolvable> errors) {
        MethodParameter parameter = Mockito.mock(MethodParameter.class);
        Mockito.when(parameter.getParameterName()).thenReturn(parameterName);
        if (headerName != null) {
            RequestHeader header = Mockito.mock(RequestHeader.class);
            Mockito.when(header.name()).thenReturn(headerName);
            Mockito.when(parameter.getParameterAnnotation(RequestHeader.class)).thenReturn(header);
        }
        if (queryName != null) {
            RequestParam query = Mockito.mock(RequestParam.class);
            Mockito.when(query.name()).thenReturn(queryName);
            Mockito.when(parameter.getParameterAnnotation(RequestParam.class)).thenReturn(query);
        }
        ParameterValidationResult parameterResult = new ParameterValidationResult(
            parameter, " ", errors, null, null, null, (error, type) -> error
        );
        MethodValidationResult result = Mockito.mock(MethodValidationResult.class);
        Mockito.when(result.getParameterValidationResults()).thenReturn(List.of(parameterResult));
        return new HandlerMethodValidationException(result);
    }
}
