package com.github.kongchen.swagger.docgen.reader;

import io.swagger.annotations.ApiOperation;
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

    private final Log log = new SystemStreamLog();

    @Test
    public void testReadWithEmptyPathOnMethodLevel() throws Exception {

        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(ExampleController.class)));

        Map<String, Path> paths = swagger.getPaths();

        Assert.assertTrue(paths.keySet().contains("/api"));
        Assert.assertEquals(paths.get("/api").getGet().getOperationId(), "emptyPathEndpoint");

        Assert.assertTrue(paths.keySet().contains("/api/someendpoint"));
        Assert.assertEquals(paths.get("/api/someendpoint").getGet().getOperationId(), "someEndpoint");
    }


    @Test
    public void testApiOperationNickname() throws Exception {

        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(ExampleController.class)));

        Path path = swagger.getPaths().get("/api/endpoint-with-nickname");
        Assert.assertEquals(path.getGet().getOperationId(), "nicknamed");
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

        @GetMapping("endpoint-with-nickname")
        @ApiOperation(nickname = "nicknamed", value = "")
        public void endpointWithNickname() {

        }
    }

}
