package com.github.kongchen.swagger.docgen.reader;

import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.singletonList;

public class SpringMvcApiReaderTest {

    @Test
    public void testReadWithEmptyPathOnMethodLevel() throws Exception {

        Log log = new SystemStreamLog();

        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(ExampleController.class)));

        Map<String, Path> paths = swagger.getPaths();

        Assert.assertTrue(paths.keySet().contains("/api"));
        Assert.assertEquals(paths.get("/api").getGet().getOperationId(), "emptyPathEndpoint");

        Assert.assertTrue(paths.keySet().contains("/api/someendpoint"));
        Assert.assertEquals(paths.get("/api/someendpoint").getGet().getOperationId(), "someEndpoint");
    }


    @RestController
    @RequestMapping("/api")
    static class ExampleController {

        @GetMapping("someendpoint")
        public void someEndpoint() {

        }

        @GetMapping("")
        public void emptyPathEndpoint() {

        }
    }

}
