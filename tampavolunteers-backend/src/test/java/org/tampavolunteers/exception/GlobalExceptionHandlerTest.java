package org.tampavolunteers.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies S-4: the catch-all handler never leaks the raw exception message.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest("GET", "/api/test"));
    }

    @Test
    void catchAll_doesNotLeakExceptionMessage() throws Exception {
        RuntimeException ex = new RuntimeException("SELECT * FROM users WHERE id=1; --internal detail");

        var response = handler.handleGlobalException(ex, webRequest());
        String body = mapper.writeValueAsString(response.getBody());

        assertThat(body).doesNotContain("SELECT");
        assertThat(body).doesNotContain("internal detail");
    }

    @Test
    void catchAll_returnsGenericMessageWithCorrelationId() throws Exception {
        RuntimeException ex = new RuntimeException("table users does not exist");

        var response = handler.handleGlobalException(ex, webRequest());
        GlobalExceptionHandler.ErrorResponse error = response.getBody();

        assertThat(error).isNotNull();
        assertThat(error.message()).contains("An unexpected error occurred");
        assertThat(error.message()).contains("Reference:");
        assertThat(error.status()).isEqualTo(500);
    }

    @Test
    void catchAll_correlationIdIsUniquePerRequest() {
        RuntimeException ex = new RuntimeException("boom");

        String msg1 = handler.handleGlobalException(ex, webRequest()).getBody().message();
        String msg2 = handler.handleGlobalException(ex, webRequest()).getBody().message();

        // Both should contain "Reference:" but with different UUIDs
        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    void badRequest_stillLeaksApplicationMessage() {
        // Application-controlled exceptions (BadRequestException) should still propagate
        // their messages — only unhandled exceptions must be sanitised.
        BadRequestException ex = new BadRequestException("Email already exists");
        var response = handler.handleBadRequestException(ex, webRequest());
        assertThat(response.getBody().message()).isEqualTo("Email already exists");
    }
}
