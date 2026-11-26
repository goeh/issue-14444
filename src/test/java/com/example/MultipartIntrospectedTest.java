package com.example;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * While trying to workaround <a href="https://github.com/micronaut-projects/micronaut-core/issues/14444">#14444</a> I stumbled upon this exception when using Uri parameters with MultipartBody:
 * <pre>
 *  No bean introspection available for type [class io.micronaut.http.client.multipart.MultipartBody]. Ensure the class is annotated with io.micronaut.core.annotation.Introspected
 *  io.micronaut.core.beans.exceptions.IntrospectionException: No bean introspection available for type [class io.micronaut.http.client.multipart.MultipartBody]. Ensure the class is annotated with io.micronaut.core.annotation.Introspected
 * 	at io.micronaut.core.beans.BeanIntrospector.lambda$getIntrospection$3(BeanIntrospector.java:116)
 * 	at java.base/java.util.Optional.orElseThrow(Optional.java:403)
 * 	at io.micronaut.core.beans.BeanIntrospector.getIntrospection(BeanIntrospector.java:116)
 * 	at io.micronaut.core.beans.BeanMap.of(BeanMap.java:47)
 * 	at io.micronaut.http.client.interceptor.HttpClientIntroductionAdvice.bindPathParams(HttpClientIntroductionAdvice.java:458)
 * 	at io.micronaut.http.client.interceptor.HttpClientIntroductionAdvice.bindRequest(HttpClientIntroductionAdvice.java:398)
 * 	at io.micronaut.http.client.interceptor.HttpClientIntroductionAdvice.handleSynchronous(HttpClientIntroductionAdvice.java:220)
 * 	at io.micronaut.http.client.interceptor.HttpClientIntroductionAdvice.intercept(HttpClientIntroductionAdvice.java:195)
 * </pre>
 */
@MicronautTest
class MultipartIntrospectedTest {

    @Test
    void queryWithMultipartBodyController() {
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class,
                Map.of("spec.name", "MultipartBodyClientTest"));
             ApplicationContext clientCtx = ApplicationContext.run(Map.of(
                     "spec.name", "MultipartBodyClientTest",
                     // no path, no trailing slash
                     "root.url", "http://localhost:" + server.getPort()))) {

            MultipartClient client = clientCtx.getBean(MultipartClient.class);
            MultipartBody file = MultipartBody.builder().addPart("file", "test.txt", MediaType.TEXT_PLAIN_TYPE, "test".getBytes(StandardCharsets.UTF_8)).build();

            assertEquals("q=x", client.uploadWithQueryValue("x", file));
            assertEquals("q=null", client.uploadWithQueryValue(null, file));

            assertEquals("q=x", client.uploadWithUriVariable("x", file));
            assertEquals("q=null", client.uploadWithUriVariable(null, file));
        }
    }

    @Requires(property = "spec.name", value = "MultipartBodyClientTest")
    @Controller
    static class RootController {
        @Post(value = "/", consumes = MediaType.MULTIPART_FORM_DATA)
        String upload(@Nullable @QueryValue String q, @Part("file") CompletedFileUpload file) {
            return "q=" + q;
        }
    }

    @Requires(property = "spec.name", value = "MultipartBodyClientTest")
    @Client("${root.url}")
    interface MultipartClient {
        @Post(value = "/", produces = MediaType.MULTIPART_FORM_DATA)
        String uploadWithQueryValue(@Nullable @QueryValue String q, @Body MultipartBody body);

        @Post(value = "/{?q}", produces = MediaType.MULTIPART_FORM_DATA)
        String uploadWithUriVariable(@Nullable String q, @Body MultipartBody body);
    }
}
