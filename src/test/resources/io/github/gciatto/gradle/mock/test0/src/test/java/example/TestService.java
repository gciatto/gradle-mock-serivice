package io.github.gciatto.gradle.mock.test0.src.test.kotlin.example;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class TestService {
    @Test
    public void test() throws MalformedURLException, IOException {
        var url = new URL("http://localhost:8080/hello");
        try (var it = new BufferedReader(new InputStreamReader(url.openStream()))) {
            var response = it.readLine();
            assertEquals("hello", response);
        }
    }
}
