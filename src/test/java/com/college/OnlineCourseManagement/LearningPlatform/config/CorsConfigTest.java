package com.college.OnlineCourseManagement.LearningPlatform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS was hardcoded to a single origin, {@code http://localhost:5173}.
 *
 * That made every API call fail whenever the frontend was opened as {@code 127.0.0.1:5173},
 * or from any other dev port or a deployed host, while the rest of the page rendered
 * normally -- so the symptom was "the catalog is empty", with nothing in the UI to explain it.
 */
@SpringBootTest
class CorsConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private Environment env;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("both loopback spellings are allowed, since browsers treat them as different origins")
    void bothLoopbackSpellingsAllowed() {
        assertThat(env.getProperty("app.cors.allowed-origins"))
                .as("CORS must cover 127.0.0.1 as well as localhost")
                .contains("http://localhost:5173")
                .contains("http://localhost:5174")
                .contains("http://127.0.0.1:5173");
    }

    @Test
    @DisplayName("a dev-server origin receives the matching Access-Control-Allow-Origin")
    void allowsDevServerOrigin() throws Exception {
        var result = mockMvc()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/courses")
                        .header("Origin", "http://127.0.0.1:5173"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .as("the header must echo the requesting origin")
                .isEqualTo("http://127.0.0.1:5173");
    }

    @Test
    @DisplayName("credentials are allowed, which the HttpOnly refresh-token cookie requires")
    void allowsCredentials() throws Exception {
        var result = mockMvc()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/courses")
                        .header("Origin", "http://localhost:5173"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials"))
                .as("the refresh token is an HttpOnly cookie, so credentials must be allowed")
                .isEqualTo("true");
    }

    @Test
    @DisplayName("an unrecognised origin is refused, so this is not an open CORS policy")
    void refusesUnknownOrigin() throws Exception {
        var result = mockMvc()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/courses")
                        .header("Origin", "http://evil.example.com"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .as("an arbitrary site must not be able to read API responses")
                .isNull();
    }
}