package com.vibe.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for JwksController.
 */
@DisplayName("JWKS Controller Tests")
class JwksControllerTest {

    private MockMvc mockMvc;
    private JwtTokenProvider jwtTokenProvider;
    private JwksController jwksController;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        jwksController = new JwksController(jwtTokenProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(jwksController).build();

        when(jwtTokenProvider.getKeyId()).thenReturn("key-2026-03-02-v1");
        when(jwtTokenProvider.getModulus()).thenReturn("mock-modulus");
        when(jwtTokenProvider.getExponent()).thenReturn("AQAB");
    }

    @Test
    @DisplayName("Should return JWKS with correct structure")
    void shouldReturnJwksWithCorrectStructure() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys", hasSize(1)));
    }

    @Test
    @DisplayName("Should include RSA key parameters")
    void shouldIncludeRsaKeyParameters() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").value("key-2026-03-02-v1"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"));
    }

    @Test
    @DisplayName("Should include modulus and exponent")
    void shouldIncludeModulusAndExponent() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].n").value("mock-modulus"))
                .andExpect(jsonPath("$.keys[0].e").value("AQAB"));
    }

    @Test
    @DisplayName("Should return JSON content type")
    void shouldReturnJsonContentType() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_JSON));
    }
}
