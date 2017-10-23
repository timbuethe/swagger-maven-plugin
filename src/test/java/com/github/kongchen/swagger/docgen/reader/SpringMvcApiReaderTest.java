package com.github.kongchen.swagger.docgen.reader;

import com.wordnik.sample.model.User;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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

    /**
     * see https://swagger.io/docs/specification/2-0/file-upload/
     */
    @Test
    public void testFileUpload() throws Exception {


        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(FileController.class)));

        Path path = swagger.getPaths().get("/v1/files/upload/{entityId}");

        Parameter parameter = path.getPost().getParameters().get(1);

        Assert.assertEquals(parameter.getIn(), "formData");
        Assert.assertEquals(parameter.getName(), "file");
    }

    @Test
    public void testFileDownload() throws Exception {

        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(FileController.class)));

        Path path = swagger.getPaths().get("/v1/files/download/{entityId}");

        Response response = path.getGet().getResponses().get("200");
        Assert.assertEquals(response.getSchema().getType(), "file");
    }

    @Test
    public void testApiParam() throws Exception {

        SpringMvcApiReader springMvcApiReader = new SpringMvcApiReader(null, log);
        Swagger swagger = springMvcApiReader.read(new HashSet<>(singletonList(ExampleController.class)));

        Operation operation = swagger.getPaths().get("/api/endpoint-with-api-param").getPost();
        Assert.assertEquals(operation.getParameters().size(), 1);

        Parameter parameter = operation.getParameters().get(0);
        Assert.assertEquals(parameter.getIn(), "body");
        Assert.assertEquals(parameter.getName(), "body");
        Assert.assertEquals(parameter.getDescription(), "List of user object");
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

        @PostMapping("endpoint-with-api-param")
        @SuppressWarnings("unused")
        public void endpointWithApiParam(@ApiParam(value = "List of user object",required = true) @RequestBody List<User> users) {

        }
    }


    @RestController
    @RequestMapping("/v1/files")
    static class FileController {

        @RequestMapping(value = "/upload/{entityId}", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = {"application/x-protobuf", "application/json"})
        public ResponseEntity<UploadFileResponseMessage> uploadFile(
                @PathVariable String entityId,
                @RequestPart("file") MultipartFile file) throws IOException {

            return new ResponseEntity<>(new UploadFileResponseMessage(), HttpStatus.CREATED);
        }

        @GetMapping(value = "/download/{entityId}")
        public byte[] downloadFile(@PathVariable String entityId) {

            return new byte[1024];
        }
    }

    private static class UploadFileResponseMessage {

    }
}
