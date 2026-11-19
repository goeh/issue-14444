package com.example;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class Issue14444Test {

    @Test
    void queryOnlyClientRequestToRootController() {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class,
                Map.of("spec.name", "RootQueryClientTest"));
             ApplicationContext clientCtx = ApplicationContext.run(Map.of(
                     "spec.name", "RootQueryClientTest",
                     // no path, no trailing slash
                     "root.url", "http://localhost:" + server.getPort()))) {

            RootClient client = clientCtx.getBean(RootClient.class);

            assertEquals("q=x", client.withTemplate("x"));   // 4.10.36: OK, 4.10.37: 400 Malformed URI
            assertEquals("q=x", client.withoutTemplate("x")); // 4.10.36: OK, 4.10.37: 400 Malformed URI
        }
    }

    @Requires(property = "spec.name", value = "RootQueryClientTest")
    @Controller
    static class RootController {
        @Get
        String list(@Nullable @QueryValue String q) {
            return "q=" + q;
        }
    }

    @Requires(property = "spec.name", value = "RootQueryClientTest")
    @Client("${root.url}")
    interface RootClient {
        @Get("{?q}")
        String withTemplate(@QueryValue String q);

        @Get
        String withoutTemplate(@QueryValue String q);
    }
}
