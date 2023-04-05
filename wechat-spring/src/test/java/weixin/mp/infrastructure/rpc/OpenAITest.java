package weixin.mp.infrastructure.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * ------------------------------------------------------------------------------------------
 * ------------------------------------------| [Q:]  bala                           [repeat]|
 * | + New Chat                              | [A:]  bala bala             [like] [dislike] |
 * ------------------------------------------| [Q:] continue...                             |
 * | Gened Conversation Name [edit] [delete] |                                              |
 * | Conversation Name       [edit] [delete] |                                              |
 * | Conversation            [edit] [delete] |                                              |
 * ------------------------------------------------------------------------------------------
 * <ol>
 *     <li>curl -X GET https://chat.openai.com/backend-api/conversations?offset=0&limit=20 --> {"items":[{"id":"f7e5fe07-bcfb-44ec-93e5-f36b6b82b33c","title":"New chat","create_time":"2023-02-27T13:47:39.241893"}],"total":0,"limit":20,"offset":0}</li>
 *     <li>curl -X GET https://chat.openai.com/backend-api/models --> {"models":[{"slug":"text-davinci-002-render-sha","max_tokens":4097,"title":"Turbo (Default for free users)","description":"The standard ChatGPT model","tags":[]}]}</li>
 *     <li>curl -X POST -d '{"action":"next","messages":[{"id":"2de7558c-a570-49eb-ab05-4e9ef5d55bec","author":{"role":"user"},"role":"user","content":{"content_type":"text","parts":["{{prompt}}"]}}],"parent_message_id":"3268ba79-1064-452a-b082-86dc9ad245cc","model":"text-davinci-002-render-sha"}' https://chat.openai.com/backend-api/conversation --> text/event-stream</li>
 *     <li>curl -X GET https://chat.openai.com/backend-api/conversations?offset=0&limit=20 --> {"items":[{"id":"a46859ce-92f4-4f5f-b36f-0bbb2581e6a5","title":"New chat","create_time":"2023-02-28T14:35:08.366386"},{}],"total":0,"limit":20,"offset":0}</li>
 *     <li>curl -X POST -d '{"input":"{{prompt}}","model":"text-moderation-playground","conversation_id":"a46859ce-92f4-4f5f-b36f-0bbb2581e6a5","message_id":"2de7558c-a570-49eb-ab05-4e9ef5d55bec"}' https://chat.openai.com/backend-api/moderations --> {"flagged":false,"blocked":false,"moderation_id":"modr-6oY8sqW5zyBsYohv5WgetAbTSKYPn"}</li>
 *     <li>curl -X POST -d '{"message_id":"03f513fc-14ca-4bd1-87d3-bf17a183ac67","model":"text-davinci-002-render-sha"}' https://chat.openai.com/backend-api/conversation/gen_title/a46859ce-92f4-4f5f-b36f-0bbb2581e6a5 --> {"title":"{{Gened Conversation Name}}"}</li>
 *     <li>curl -X POST -d '{"input":"\n{{completions}}","model":"text-moderation-playground","conversation_id":"a46859ce-92f4-4f5f-b36f-0bbb2581e6a5","message_id":"03f513fc-14ca-4bd1-87d3-bf17a183ac67"}' https://chat.openai.com/backend-api/moderations --> {"flagged":false,"blocked":false,"moderation_id":"modr-6oYALJkV13Aj4QLx6AWhHwG90jfxq"} </li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
public class OpenAITest {

    @Mock
    private ClientHttpConnector clientHttpConnector;

    @Mock
    private ClientHttpResponse clientHttpResponse;

    private OpenAI openAI;

    private final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    @BeforeEach
    public void setUp() {
        openAI = new OpenAI.JavaClient(WebClient.builder().clientConnector(clientHttpConnector).build());
        headers.add("Content-Type", "application/json");
        headers.add("Access-Control-Allow", "*");
        headers.add("Openai-Organization", "user-hbhrhl9gevoojum9hps0w3xt");
        headers.add("Openai-Processing-Ms", String.valueOf(2900));
        headers.add("Openai-Version", "2020-10-01");
        headers.add("Strict-Transport-Security", "max-age=15724800; includeSubDomains");
        headers.add("X-Request-Id", "9941dca4a7d07a91bde3a9cac5ede520");
    }

    @AfterEach
    protected void tearDown() {
        Mockito.reset(clientHttpConnector);
        Mockito.reset(clientHttpResponse);
    }

    @Test
    public void testListModel() {
        String jsonFile = "src/test/resources/openai/models.json";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(Path.of(jsonFile), StandardOpenOption.READ), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.Response<OpenAI.ModelResponse> response = openAI.listModel().join();
        Assertions.assertEquals("list", response.getObject());
        Assertions.assertTrue(response.getData().size() > 0);
        Assertions.assertTrue(response.getData().stream().anyMatch(m -> "text-davinci-003".equals(m.id())));
        Assertions.assertEquals(0, response.getData().stream().filter(m -> "curie:ft-personal-2023-02-22-14-00-19".equals(m.id())).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"text-davinci-003"})
    public void testRetrieveModel(String model) {
        String response = """
                {
                  "id": "text-davinci-003",
                  "object": "model",
                  "created": 1669599635,
                  "owned_by": "openai-internal",
                  "permission": [
                    {
                      "id": "modelperm-loLaKHUdKtFOPD6zujUCDHno",
                      "object": "model_permission",
                      "created": 1677093237,
                      "allow_create_engine": false,
                      "allow_sampling": true,
                      "allow_logprobs": true,
                      "allow_search_indices": false,
                      "allow_view": true,
                      "allow_fine_tuning": false,
                      "organization": "*",
                      "group": null,
                      "is_blocking": false
                    }
                  ],
                  "root": "text-davinci-003",
                  "parent": null
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.ModelResponse result = openAI.retrieveModel(model).join();
        Assertions.assertEquals(model, result.id());
        Assertions.assertEquals("model", result.object());
        Assertions.assertTrue(result.permission().size() > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"curie:ft-personal-2023-02-22-14-00-19", "text-davinci-003"})
    public void testCompletion(String model) {
        String response = """
                {"id":"cmpl-6n62YdWdhWSPcubbuJaAbtXswj2By","object":"text_completion","created":1677159310,"model":"text-davinci-003","choices":[{"text":"图\\n\\n双标图又称双坐标图，是在一张图上同时使用两个反应不同特征的不同坐标系统，以表示不同尺度的多变量关系，可以对比分析两个变量间的联系，更加直观地展示现象，从而加深理解和评价变量之间的精确关系。双标图经常用于分析研究两个变量之间关系，如货","index":0,"logprobs":null,"finish_reason":"length"}],"usage":{"prompt_tokens":9,"completion_tokens":256,"total_tokens":265}}""";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>(headers);
        mvm.add("Openai-Model", model);
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(mvm));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.CompletionRequest request = OpenAI.CompletionRequest.of(model, "什么是双标", 256, null);
        OpenAI.TextResponse result = openAI.complete(request).join();
        Assertions.assertEquals("text_completion", result.object());
//        Assertions.assertEquals(model, result.model());
        if (request.n() != null) {
            Assertions.assertTrue(result.choices().size() <= request.n());
        } else {
            Assertions.assertEquals(1, result.choices().size());
        }
        Assertions.assertTrue(result.usage().completionTokens() <= request.maxTokens());
    }

    @ParameterizedTest
    @ValueSource(strings = {"gpt-3.5-turbo", "gpt-3.5-turbo-0301"})
    public void testChat(String model) throws Exception {
        String response = """
                {"id":"chatcmpl-6qF5s2r6nzNNMTyDy7igNCQaFZ0ua","object":"chat.completion","created":1677909096,"model":"gpt-3.5-turbo-0301","usage":{"prompt_tokens":14,"completion_tokens":175,"total_tokens":189},"choices":[{"message":{"role":"assistant","content":"\\n\\n双标是指一个人或团体在不同的情境或人群面前，对同一个问题或行为给出不同的标准或两面性的态度和看法。双标现象在社交媒体上尤为普遍，经常出现在公众人物或媒体人士的言论行为中。例如，某些公众人物曾公开表示反对某种行为或说法，而在另一种场合或面对另一些人群时，则表现出与前者完全相反的态度和看法，这就是典型的双标行为。"},"finish_reason":"stop","index":0}]}""";
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>(headers);
        mvm.add("Openai-Model", model);
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(mvm));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.ChatRequest request = OpenAI.ChatRequest.withDefault(model, List.of(new OpenAI.ChatMessage(OpenAI.Role.USER, "什么是双标")));
        OpenAI.ChatResponse result = openAI.chat(request).get();
        Assertions.assertNotNull(result.id());
        Assertions.assertEquals("chat.completion", result.object());
        if (request.n() == null) {
            Assertions.assertEquals(1, result.choices().size());
        } else {
            Assertions.assertEquals(request.n(), result.choices().size());
        }
        Assertions.assertEquals(OpenAI.Role.AI, result.choices().get(0).message().role());
        Assertions.assertNotNull(result.choices().get(0).message().content());
        Assertions.assertTrue(result.usage().completionTokens() >= result.choices().get(0).message().content().length());
    }

    @Test
    public void testEditText() {
        // '\n' should replace with '\\n' or 'org.springframework.core.codec.DecodingException: JSON decoding error: Illegal unquoted character ((CTRL-CHAR, code 10)): has to be escaped using backslash to be included in string value'
        String response = "{\"object\":\"edit\",\"created\":1677159758,\"choices\":[{\"text\":\":兴，百姓兴。亡，百姓亡\\n\",\"index\":0}],\"usage\":{\"prompt_tokens\":60,\"completion_tokens\":63,\"total_tokens\":123}}";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.EditTextRequest request = OpenAI.EditTextRequest.of("text-davinci-edit-001", "兴，百姓苦。亡，百牲苦", "修正元曲内容的错别字");
        OpenAI.TextResponse result = openAI.edit(request).join();
        Assertions.assertEquals("edit", result.object());
        if (request.n() != null) {
            Assertions.assertTrue(result.choices().size() <= request.n());
        } else {
            Assertions.assertEquals(1, result.choices().size());
        }
    }

    @Test
    public void testGeneration() {
        String response = """
                {
                  "created": 1677160532,
                  "data": [
                    {
                      "b64_json": "ALongLongDataWhichPresentImageYouCanVisitFileUnderSrcTestResourcesChatGPT1677160532ForContent"
                    }
                  ]
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.ImageGenerationRequest request = new OpenAI.ImageGenerationRequest("独立寒秋，湘江北去，橘子洲头。看万山红遍，层林尽染；漫江碧透，百舸争流。鹰击长空，鱼翔浅底，万类霜天竞自由。", 1, OpenAI.ImageSize.SMALL, OpenAI.ImageResponseFormat.BASE64_JSON, null);
        OpenAI.ImageResponse result = openAI.generate(request).join();
        if (request.n() != null) {
            Assertions.assertTrue(result.data().size() <= request.n());
        } else {
            Assertions.assertEquals(1, result.data().size());
        }
        if (request.responseFormat() == OpenAI.ImageResponseFormat.BASE64_JSON) {
            Assertions.assertNotNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNull(result.data().get(0).url());
        } else {
            Assertions.assertNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNotNull(result.data().get(0).url());
        }
    }

    @Test
    public void testEditImage() throws Exception {
//        FileInputStream fis = new FileInputStream("src/test/resources/openai/1677160532.png");
//        BufferedImage image = ImageIO.read(fis);
//        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
//        bufferedImage.getGraphics().drawImage(image, 0, 0, null);
//        for (int x = bufferedImage.getMinX(); x < bufferedImage.getWidth(); x++) {
//            for (int y = bufferedImage.getMinY(); y < bufferedImage.getHeight(); y++) {
//                int rgb = bufferedImage.getRGB(x, y);
//                bufferedImage.setRGB(x, y, rgb & 0x00ffffff);
//            }
//        }
//        ImageIO.write(bufferedImage, "png", new File("src/test/resources/openai/1677160532-alpha.png"));

        String response = """
                {
                  "created": 1677417889,
                  "data": [
                    {
                      "url": "https://oaidalleapiprodscus.blob.core.windows.net/private/org-scLMFVYrYyin6oFpv38dnCGr/user-HbHRHL9GEvOoJUM9HPs0w3Xt/img-g51WdjAhbSD5pXBwSlY7T1TD.png?st=2023-02-26T12%3A24%3A49Z&se=2023-02-26T14%3A24%3A49Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-02-25T21%3A46%3A47Z&ske=2023-02-26T21%3A46%3A47Z&sks=b&skv=2021-08-06&sig=4l8UKrklGZ1iLytAuKUfBcxWmJdbtQqVnlxSUXLvCD4%3D"
                    }
                  ]
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.EditImageRequest request = OpenAI.EditImageRequest.withDefault(new FileInputStream("src/test/resources/openai/1677160532-alpha.png"), "1677160532-alpha.png", "毛泽东塑像");
        OpenAI.ImageResponse result = openAI.edit(request).join();
        if (request.n() != null) {
            Assertions.assertTrue(result.data().size() <= request.n());
        } else {
            Assertions.assertEquals(1, result.data().size());
        }
        if (request.responseFormat() == OpenAI.ImageResponseFormat.BASE64_JSON) {
            Assertions.assertNotNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNull(result.data().get(0).url());
        } else {
            Assertions.assertNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNotNull(result.data().get(0).url());
        }
    }

    @Test
    public void testMutate() throws Exception {
        String response = """
                {
                  "created": 1677415466,
                  "data": [
                    {
                      "url": "https://oaidalleapiprodscus.blob.core.windows.net/private/org-scLMFVYrYyin6oFpv38dnCGr/user-HbHRHL9GEvOoJUM9HPs0w3Xt/img-YatOujPlZKoK0D2Naf9Jml9K.png?st=2023-02-26T11%3A44%3A26Z&se=2023-02-26T13%3A44%3A26Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-02-26T11%3A07%3A48Z&ske=2023-02-27T11%3A07%3A48Z&sks=b&skv=2021-08-06&sig=bcp//u6fh8Gs1PyoqjyA/OLApxIWspOaNOsp6dmPSeg%3D"
                    }
                  ]
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.VariantImageRequest request = OpenAI.VariantImageRequest.withDefault(new FileInputStream("src/test/resources/openai/1677160532.png"), "1677160532.png");
        OpenAI.ImageResponse result = openAI.mutate(request).join();
        if (request.n() != null) {
            Assertions.assertTrue(result.data().size() <= request.n());
        } else {
            Assertions.assertEquals(1, result.data().size());
        }
        if (request.responseFormat() == OpenAI.ImageResponseFormat.BASE64_JSON) {
            Assertions.assertNotNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNull(result.data().get(0).url());
        } else {
            Assertions.assertNull(result.data().get(0).base64EncodedImage());
            Assertions.assertNotNull(result.data().get(0).url());
        }
    }

    @Test
    public void testEmbedding() {
        String error = """
                {
                    "error": {
                        "message": "You are not allowed to generate embeddings from this model",
                        "type": "invalid_request_error",
                        "param": null,
                        "code": null
                    }
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.FORBIDDEN);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(error.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        try {
            openAI.embedding(new OpenAI.EmbeddingRequest("text-davinci-003", "知之为知之，不知为不知，是智也", null)).join();
        } catch (CompletionException e) {
            Throwable t = e.getCause();
            Assertions.assertTrue(t instanceof WebClientResponseException.Forbidden);
            OpenAI.ErrorResponse err = ((WebClientResponseException.Forbidden) t).getResponseBodyAs(OpenAI.ErrorResponse.class);
            Assertions.assertNotNull(err.error());
        }
    }

    @Test
    public void testTranscribe() throws Exception {
        String response = "{\"text\":\"燦哥們,生華,人民生活好 咱要人民生活好 心靈振翼\"}";

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.TranscribeRequest request = new OpenAI.TranscribeRequest(new FileInputStream("src/test/resources/openai/1949FoundingCeremony.mp3"), "1949FoundingCeremony.mp3", "whisper-1");
        OpenAI.TranscriptionResponse result = openAI.transcribe(request).get();
        if (request.getResponseFormat() == null) {
            Assertions.assertTrue(result instanceof OpenAI.TranscriptionJsonResponse);
            Assertions.assertTrue(((OpenAI.TranscriptionJsonResponse) result).text().contains("人民"));
        }

    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "text", "srt", "verbose_json", "vtt"})
    public void testTranslation(String responseFormat) throws Exception {
        // json, response content-type=application/json
        String jsonResponse = """
                {"text":"Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio. Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke. Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals. A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be. The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed. With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan, visited the region on Thursday to assess the ongoing response and hear from impacted residents. Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us. We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available. More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town, state health officials are urging some East Palestine residents to drink bottled water until water tests are complete. Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe. But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles, including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property. In the back of your property back here, they found dead fish? Yeah, they saw dead fish. Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment. She says she's still waiting for the state to come and test her well water. Air wise, I feel okay. Water wise, no. I, no. There's just too many chemicals and stuff that were spilled that they still don't want to identify completely. An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment. These people saw the flames from their homes and worry their neighborhood still may not be safe. What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground. I mean, vegetables or tomatoes or anything this year because we don't know. I don't think they're going to do enough. And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground. We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks. They're not telling us why. And this is daily. I'm driving my children to school past all of this. And they're asking me questions that I don't have answers to. Some of their questions unanswered. We found getting information just as challenging."}""";
        // text, response content-type=text/plain;charset=utf-8
        String textResponse = "Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio. Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke. Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals. A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be. The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed. With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan, visited the region on Thursday to assess the ongoing response and hear from impacted residents. Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us. We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available. More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town, state health officials are urging some East Palestine residents to drink bottled water until water tests are complete. Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe. But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles, including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property. In the back of your property back here, they found dead fish? Yeah, they saw dead fish. Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment. She says she's still waiting for the state to come and test her well water. Air wise, I feel okay. Water wise, no. I, no. There's just too many chemicals and stuff that were spilled that they still don't want to identify completely. An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment. These people saw the flames from their homes and worry their neighborhood still may not be safe. What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground. I mean, vegetables or tomatoes or anything this year because we don't know. I don't think they're going to do enough. And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground. We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks. They're not telling us why. And this is daily. I'm driving my children to school past all of this. And they're asking me questions that I don't have answers to. Some of their questions unanswered. We found getting information just as challenging.";
        // srt, response content-type=text/plain;charset=utf-8
        String srtResponse = """
                1
                00:00:00,000 --> 00:00:08,500
                Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio.
                                
                2
                00:00:08,500 --> 00:00:14,500
                Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke.
                                
                3
                00:00:14,500 --> 00:00:23,000
                Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals.
                                
                4
                00:00:23,000 --> 00:00:30,500
                A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be.
                                
                5
                00:00:30,500 --> 00:00:38,000
                The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed.
                                
                6
                00:00:38,000 --> 00:00:44,500
                With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan,
                                
                7
                00:00:44,500 --> 00:00:50,500
                visited the region on Thursday to assess the ongoing response and hear from impacted residents.
                                
                8
                00:00:50,500 --> 00:00:57,500
                Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us.
                                
                9
                00:00:57,500 --> 00:01:09,000
                We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available.
                                
                10
                00:01:09,000 --> 00:01:15,500
                More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town,
                                
                11
                00:01:15,500 --> 00:01:24,000
                state health officials are urging some East Palestine residents to drink bottled water until water tests are complete.
                                
                12
                00:01:24,000 --> 00:01:32,500
                Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe.
                                
                13
                00:01:32,500 --> 00:01:38,500
                But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles,
                                
                14
                00:01:38,500 --> 00:01:47,500
                including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property.
                                
                15
                00:01:47,500 --> 00:01:51,500
                In the back of your property back here, they found dead fish? Yeah, they saw dead fish.
                                
                16
                00:01:51,500 --> 00:02:00,500
                Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment.
                                
                17
                00:02:00,500 --> 00:02:04,500
                She says she's still waiting for the state to come and test her well water.
                                
                18
                00:02:04,500 --> 00:02:09,500
                Air wise, I feel okay. Water wise, no. I, no.
                                
                19
                00:02:09,500 --> 00:02:14,500
                There's just too many chemicals and stuff that were spilled that they still don't want to identify completely.
                                
                20
                00:02:14,500 --> 00:02:23,500
                An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment.
                                
                21
                00:02:23,500 --> 00:02:29,500
                These people saw the flames from their homes and worry their neighborhood still may not be safe.
                                
                22
                00:02:29,500 --> 00:02:36,500
                What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground.
                                
                23
                00:02:36,500 --> 00:02:41,500
                I mean, vegetables or tomatoes or anything this year because we don't know.
                                
                24
                00:02:41,500 --> 00:02:43,500
                I don't think they're going to do enough.
                                
                25
                00:02:43,500 --> 00:02:50,500
                And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground.
                                
                26
                00:02:50,500 --> 00:02:55,500
                We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks.
                                
                27
                00:02:55,500 --> 00:03:01,500
                They're not telling us why. And this is daily. I'm driving my children to school past all of this.
                                
                28
                00:03:01,500 --> 00:03:04,500
                And they're asking me questions that I don't have answers to.
                                
                29
                00:03:04,500 --> 00:03:25,500
                Some of their questions unanswered. We found getting information just as challenging.""";
        // verbose_json, response content-type=application/json
        String verboseJsonResponse = "{\"task\":\"translate\",\"language\":\"english\",\"duration\":189.79,\"segments\":[{\"id\":0,\"seek\":0,\"start\":0.0,\"end\":8.5,\"text\":\" Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio.\",\"tokens\":[3087,1657,965,13,24552,341,1618,11,257,6966,39705,13724,3847,11,597,390,9792,12786,16152,412,264,565,11,1163,24731,490,264,10218,293,24190,294,19346,14469,13],\"temperature\":0.0,\"avg_logprob\":-0.28965579138861763,\"compression_ratio\":1.5907335907335907,\"no_speech_prob\":0.009554696269333363,\"transient\":false},{\"id\":1,\"seek\":0,\"start\":8.5,\"end\":14.5,\"text\":\" Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke.\",\"tokens\":[823,11,264,1163,864,518,5335,1226,264,40020,16152,322,3150,11,10322,264,3954,295,6747,33030,365,8439,13],\"temperature\":0.0,\"avg_logprob\":-0.28965579138861763,\"compression_ratio\":1.5907335907335907,\"no_speech_prob\":0.009554696269333363,\"transient\":false},{\"id\":2,\"seek\":0,\"start\":14.5,\"end\":23.0,\"text\":\" Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals.\",\"tokens\":[20216,1088,2942,364,42740,6668,926,264,8252,11,293,579,1921,257,2563,15673,11,10332,257,10164,4374,295,264,16152,13],\"temperature\":0.0,\"avg_logprob\":-0.28965579138861763,\"compression_ratio\":1.5907335907335907,\"no_speech_prob\":0.009554696269333363,\"transient\":false},{\"id\":3,\"seek\":2300,\"start\":23.0,\"end\":30.5,\"text\":\" A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be.\",\"tokens\":[316,1326,3259,1780,11,9630,362,668,14658,281,2736,1280,11,457,456,366,3195,295,1651,466,445,577,19727,10420,281,613,16152,727,312,13],\"temperature\":0.0,\"avg_logprob\":-0.1992663603562575,\"compression_ratio\":1.5955414012738853,\"no_speech_prob\":0.0001071506121661514,\"transient\":false},{\"id\":4,\"seek\":2300,\"start\":30.5,\"end\":38.0,\"text\":\" The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed.\",\"tokens\":[440,499,10018,295,8439,490,264,15673,9094,264,40020,2527,666,264,1988,293,1281,281,597,9630,576,312,9495,13],\"temperature\":0.0,\"avg_logprob\":-0.1992663603562575,\"compression_ratio\":1.5955414012738853,\"no_speech_prob\":0.0001071506121661514,\"transient\":false},{\"id\":5,\"seek\":2300,\"start\":38.0,\"end\":44.5,\"text\":\" With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan,\",\"tokens\":[2022,439,2575,322,14469,11,264,624,13,50,13,27813,25981,21649,25529,11,5911,2570,382,27447,5116,4791,282,11],\"temperature\":0.0,\"avg_logprob\":-0.1992663603562575,\"compression_ratio\":1.5955414012738853,\"no_speech_prob\":0.0001071506121661514,\"transient\":false},{\"id\":6,\"seek\":2300,\"start\":44.5,\"end\":50.5,\"text\":\" visited the region on Thursday to assess the ongoing response and hear from impacted residents.\",\"tokens\":[11220,264,4458,322,10383,281,5877,264,10452,4134,293,1568,490,15653,9630,13],\"temperature\":0.0,\"avg_logprob\":-0.1992663603562575,\"compression_ratio\":1.5955414012738853,\"no_speech_prob\":0.0001071506121661514,\"transient\":false},{\"id\":7,\"seek\":5050,\"start\":50.5,\"end\":57.5,\"text\":\" Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us.\",\"tokens\":[1692,311,24859,311,4048,44406,11,11181,48456,11,567,311,668,322,264,2727,294,14469,337,264,1036,1326,1708,11,10322,341,1657,337,505,13],\"temperature\":0.0,\"avg_logprob\":-0.20559111701117622,\"compression_ratio\":1.5755102040816327,\"no_speech_prob\":1.1477199223008938e-05,\"transient\":false},{\"id\":8,\"seek\":5050,\"start\":57.5,\"end\":69.0,\"text\":\" We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available.\",\"tokens\":[492,434,10613,30559,729,567,362,406,1939,632,641,1281,4009,10033,281,764,2274,1493,1281,13,28479,1493,1281,307,885,1027,2435,13],\"temperature\":0.0,\"avg_logprob\":-0.20559111701117622,\"compression_ratio\":1.5755102040816327,\"no_speech_prob\":1.1477199223008938e-05,\"transient\":false},{\"id\":9,\"seek\":5050,\"start\":69.0,\"end\":75.5,\"text\":\" More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town,\",\"tokens\":[5048,813,257,1243,934,257,12786,3847,1163,864,518,300,4684,281,264,42740,295,709,295,341,1359,14469,3954,11],\"temperature\":0.0,\"avg_logprob\":-0.20559111701117622,\"compression_ratio\":1.5755102040816327,\"no_speech_prob\":1.1477199223008938e-05,\"transient\":false},{\"id\":10,\"seek\":7550,\"start\":75.5,\"end\":84.0,\"text\":\" state health officials are urging some East Palestine residents to drink bottled water until water tests are complete.\",\"tokens\":[1785,1585,9798,366,48489,512,6747,33030,9630,281,2822,2274,1493,1281,1826,1281,6921,366,3566,13],\"temperature\":0.0,\"avg_logprob\":-0.19765898469206575,\"compression_ratio\":1.6108597285067874,\"no_speech_prob\":6.604307418456301e-05,\"transient\":false},{\"id\":11,\"seek\":7550,\"start\":84.0,\"end\":92.5,\"text\":\" Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe.\",\"tokens\":[11511,12356,584,264,12786,22044,390,11611,16212,264,786,934,264,1163,864,518,11,293,300,6921,362,4898,264,1988,3125,307,3273,13],\"temperature\":0.0,\"avg_logprob\":-0.19765898469206575,\"compression_ratio\":1.6108597285067874,\"no_speech_prob\":6.604307418456301e-05,\"transient\":false},{\"id\":12,\"seek\":7550,\"start\":92.5,\"end\":98.5,\"text\":\" But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles,\",\"tokens\":[583,436,362,1352,2295,4358,295,27562,1719,294,1451,11184,1281,942,47626,3407,293,257,1922,6193,11],\"temperature\":0.0,\"avg_logprob\":-0.19765898469206575,\"compression_ratio\":1.6108597285067874,\"no_speech_prob\":6.604307418456301e-05,\"transient\":false},{\"id\":13,\"seek\":9850,\"start\":98.5,\"end\":107.5,\"text\":\" including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property.\",\"tokens\":[3009,28140,8950,11,257,41868,597,6676,807,6747,33030,11,293,31521,19103,3420,11,558,807,264,646,295,30740,49474,311,4707,13],\"temperature\":0.0,\"avg_logprob\":-0.18255684262230282,\"compression_ratio\":1.7251908396946565,\"no_speech_prob\":0.0001823117199819535,\"transient\":false},{\"id\":14,\"seek\":9850,\"start\":107.5,\"end\":111.5,\"text\":\" In the back of your property back here, they found dead fish? Yeah, they saw dead fish.\",\"tokens\":[682,264,646,295,428,4707,646,510,11,436,1352,3116,3506,30,865,11,436,1866,3116,3506,13],\"temperature\":0.0,\"avg_logprob\":-0.18255684262230282,\"compression_ratio\":1.7251908396946565,\"no_speech_prob\":0.0001823117199819535,\"transient\":false},{\"id\":15,\"seek\":9850,\"start\":111.5,\"end\":120.5,\"text\":\" Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment.\",\"tokens\":[49474,1619,750,575,668,7583,2274,1493,1281,2602,295,731,1281,1562,1670,750,1409,4008,783,3116,3506,294,264,41868,3480,264,1163,864,518,13],\"temperature\":0.0,\"avg_logprob\":-0.18255684262230282,\"compression_ratio\":1.7251908396946565,\"no_speech_prob\":0.0001823117199819535,\"transient\":false},{\"id\":16,\"seek\":9850,\"start\":120.5,\"end\":124.5,\"text\":\" She says she's still waiting for the state to come and test her well water.\",\"tokens\":[1240,1619,750,311,920,3806,337,264,1785,281,808,293,1500,720,731,1281,13],\"temperature\":0.0,\"avg_logprob\":-0.18255684262230282,\"compression_ratio\":1.7251908396946565,\"no_speech_prob\":0.0001823117199819535,\"transient\":false},{\"id\":17,\"seek\":12450,\"start\":124.5,\"end\":129.5,\"text\":\" Air wise, I feel okay. Water wise, no. I, no.\",\"tokens\":[5774,10829,11,286,841,1392,13,8772,10829,11,572,13,286,11,572,13],\"temperature\":0.0,\"avg_logprob\":-0.19539737701416016,\"compression_ratio\":1.5258964143426295,\"no_speech_prob\":7.367983926087618e-05,\"transient\":false},{\"id\":18,\"seek\":12450,\"start\":129.5,\"end\":134.5,\"text\":\" There's just too many chemicals and stuff that were spilled that they still don't want to identify completely.\",\"tokens\":[821,311,445,886,867,16152,293,1507,300,645,37833,300,436,920,500,380,528,281,5876,2584,13],\"temperature\":0.0,\"avg_logprob\":-0.19539737701416016,\"compression_ratio\":1.5258964143426295,\"no_speech_prob\":7.367983926087618e-05,\"transient\":false},{\"id\":19,\"seek\":12450,\"start\":134.5,\"end\":143.5,\"text\":\" An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment.\",\"tokens\":[1107,14469,5982,295,20137,29706,4783,20561,512,805,11,7526,3506,294,264,1785,362,4539,3480,264,3847,1163,864,518,13],\"temperature\":0.0,\"avg_logprob\":-0.19539737701416016,\"compression_ratio\":1.5258964143426295,\"no_speech_prob\":7.367983926087618e-05,\"transient\":false},{\"id\":20,\"seek\":12450,\"start\":143.5,\"end\":149.5,\"text\":\" These people saw the flames from their homes and worry their neighborhood still may not be safe.\",\"tokens\":[1981,561,1866,264,23743,490,641,7388,293,3292,641,7630,920,815,406,312,3273,13],\"temperature\":0.0,\"avg_logprob\":-0.19539737701416016,\"compression_ratio\":1.5258964143426295,\"no_speech_prob\":7.367983926087618e-05,\"transient\":false},{\"id\":21,\"seek\":14950,\"start\":149.5,\"end\":156.5,\"text\":\" What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground.\",\"tokens\":[708,466,4997,1281,420,2727,30,6693,1939,13,400,286,2041,300,286,500,380,2748,291,829,1340,294,264,2727,13],\"temperature\":0.0,\"avg_logprob\":-0.21015799672980057,\"compression_ratio\":1.728937728937729,\"no_speech_prob\":0.00011234550038352609,\"transient\":false},{\"id\":22,\"seek\":14950,\"start\":156.5,\"end\":161.5,\"text\":\" I mean, vegetables or tomatoes or anything this year because we don't know.\",\"tokens\":[286,914,11,9320,420,15135,420,1340,341,1064,570,321,500,380,458,13],\"temperature\":0.0,\"avg_logprob\":-0.21015799672980057,\"compression_ratio\":1.728937728937729,\"no_speech_prob\":0.00011234550038352609,\"transient\":false},{\"id\":23,\"seek\":14950,\"start\":161.5,\"end\":163.5,\"text\":\" I don't think they're going to do enough.\",\"tokens\":[286,500,380,519,436,434,516,281,360,1547,13],\"temperature\":0.0,\"avg_logprob\":-0.21015799672980057,\"compression_ratio\":1.728937728937729,\"no_speech_prob\":0.00011234550038352609,\"transient\":false},{\"id\":24,\"seek\":14950,\"start\":163.5,\"end\":170.5,\"text\":\" And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground.\",\"tokens\":[400,512,9630,584,436,362,668,15751,538,437,436,6786,382,257,5011,295,6101,365,9798,322,264,2727,13],\"temperature\":0.0,\"avg_logprob\":-0.21015799672980057,\"compression_ratio\":1.728937728937729,\"no_speech_prob\":0.00011234550038352609,\"transient\":false},{\"id\":25,\"seek\":14950,\"start\":170.5,\"end\":175.5,\"text\":\" We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks.\",\"tokens\":[492,1320,439,295,264,48895,1694,293,456,311,7260,934,7260,365,2418,276,4201,293,2211,276,4201,439,807,264,48895,1694,13],\"temperature\":0.0,\"avg_logprob\":-0.21015799672980057,\"compression_ratio\":1.728937728937729,\"no_speech_prob\":0.00011234550038352609,\"transient\":false},{\"id\":26,\"seek\":17550,\"start\":175.5,\"end\":181.5,\"text\":\" They're not telling us why. And this is daily. I'm driving my children to school past all of this.\",\"tokens\":[814,434,406,3585,505,983,13,400,341,307,5212,13,286,478,4840,452,2227,281,1395,1791,439,295,341,13],\"temperature\":0.0,\"avg_logprob\":-0.17948513343685962,\"compression_ratio\":1.4819277108433735,\"no_speech_prob\":1.3006370863877237e-05,\"transient\":false},{\"id\":27,\"seek\":17550,\"start\":181.5,\"end\":184.5,\"text\":\" And they're asking me questions that I don't have answers to.\",\"tokens\":[400,436,434,3365,385,1651,300,286,500,380,362,6338,281,13],\"temperature\":0.0,\"avg_logprob\":-0.17948513343685962,\"compression_ratio\":1.4819277108433735,\"no_speech_prob\":1.3006370863877237e-05,\"transient\":false},{\"id\":28,\"seek\":18450,\"start\":184.5,\"end\":205.5,\"text\":\" Some of their questions unanswered. We found getting information just as challenging.\",\"tokens\":[50364,2188,295,641,1651,517,43904,292,13,492,1352,1242,1589,445,382,7595,13,51414],\"temperature\":0.0,\"avg_logprob\":-0.1994994565060264,\"compression_ratio\":1.0759493670886076,\"no_speech_prob\":6.400841084541753e-05,\"transient\":false}],\"text\":\"Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio. Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke. Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals. A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be. The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed. With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan, visited the region on Thursday to assess the ongoing response and hear from impacted residents. Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us. We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available. More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town, state health officials are urging some East Palestine residents to drink bottled water until water tests are complete. Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe. But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles, including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property. In the back of your property back here, they found dead fish? Yeah, they saw dead fish. Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment. She says she's still waiting for the state to come and test her well water. Air wise, I feel okay. Water wise, no. I, no. There's just too many chemicals and stuff that were spilled that they still don't want to identify completely. An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment. These people saw the flames from their homes and worry their neighborhood still may not be safe. What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground. I mean, vegetables or tomatoes or anything this year because we don't know. I don't think they're going to do enough. And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground. We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks. They're not telling us why. And this is daily. I'm driving my children to school past all of this. And they're asking me questions that I don't have answers to. Some of their questions unanswered. We found getting information just as challenging.\"}";
        // vtt, response content-type=text/plain;charset=utf-8
        String vttResponse = """
                WEBVTT
                                
                00:00:00.000 --> 00:00:08.500
                Next story today. Earlier this month, a Norfolk Southern train, which was carrying toxic chemicals at the time, derailed from the tracks and crashed in eastern Ohio.
                                
                00:00:08.500 --> 00:00:14.500
                Now, the derailment ignited the hazardous chemicals on board, covering the town of East Palestine with smoke.
                                
                00:00:14.500 --> 00:00:23.000
                Authorities created an evacuation zone around the crash, and fearing a major explosion, performed a controlled release of the chemicals.
                                
                00:00:23.000 --> 00:00:30.500
                A few weeks later, residents have been encouraged to return home, but there are lots of questions about just how harmful exposure to these chemicals could be.
                                
                00:00:30.500 --> 00:00:38.000
                The plumes of smoke from the explosion carried the hazardous material into the air and water to which residents would be exposed.
                                
                00:00:38.000 --> 00:00:44.500
                With all eyes on Ohio, the U.S. Environmental Protection Agency administrator, otherwise known as EPA Michael Regan,
                                
                00:00:44.500 --> 00:00:50.500
                visited the region on Thursday to assess the ongoing response and hear from impacted residents.
                                
                00:00:50.500 --> 00:00:57.500
                Here's CNN's national correspondent, Jason Carroll, who's been on the ground in Ohio for the last few days, covering this story for us.
                                
                00:00:57.500 --> 00:01:09.000
                We're strongly recommending those who have not yet had their water source checked to use bottled water. Bottled water is being made available.
                                
                00:01:09.000 --> 00:01:15.500
                More than a week after a toxic train derailment that led to the evacuation of much of this small Ohio town,
                                
                00:01:15.500 --> 00:01:24.000
                state health officials are urging some East Palestine residents to drink bottled water until water tests are complete.
                                
                00:01:24.000 --> 00:01:32.500
                Officials say the toxic spill was largely contained the day after the derailment, and that tests have shown the air quality is safe.
                                
                00:01:32.500 --> 00:01:38.500
                But they have found low levels of contaminants in four nearby waterways spanning seven and a half miles,
                                
                00:01:38.500 --> 00:01:47.500
                including Leslie Run, a creek which runs through East Palestine, and neighboring Negley, right through the back of Kathy Reese's property.
                                
                00:01:47.500 --> 00:01:51.500
                In the back of your property back here, they found dead fish? Yeah, they saw dead fish.
                                
                00:01:51.500 --> 00:02:00.500
                Reese says she has been drinking bottled water instead of well water ever since she started spotting dead fish in the creek following the derailment.
                                
                00:02:00.500 --> 00:02:04.500
                She says she's still waiting for the state to come and test her well water.
                                
                00:02:04.500 --> 00:02:09.500
                Air wise, I feel okay. Water wise, no. I, no.
                                
                00:02:09.500 --> 00:02:14.500
                There's just too many chemicals and stuff that were spilled that they still don't want to identify completely.
                                
                00:02:14.500 --> 00:02:23.500
                An Ohio Department of Natural Resources official estimates some 3,500 fish in the state have died following the train derailment.
                                
                00:02:23.500 --> 00:02:29.500
                These people saw the flames from their homes and worry their neighborhood still may not be safe.
                                
                00:02:29.500 --> 00:02:36.500
                What about testing water or ground? Nothing yet. And I guess that I don't recommend you put anything in the ground.
                                
                00:02:36.500 --> 00:02:41.500
                I mean, vegetables or tomatoes or anything this year because we don't know.
                                
                00:02:41.500 --> 00:02:43.500
                I don't think they're going to do enough.
                                
                00:02:43.500 --> 00:02:50.500
                And some residents say they have been frustrated by what they describe as a lack of communication with officials on the ground.
                                
                00:02:50.500 --> 00:02:55.500
                We pass all of the creeks and there's crew after crew with white hoses and black hoses all through the creeks.
                                
                00:02:55.500 --> 00:03:01.500
                They're not telling us why. And this is daily. I'm driving my children to school past all of this.
                                
                00:03:01.500 --> 00:03:04.500
                And they're asking me questions that I don't have answers to.
                                
                00:03:04.500 --> 00:03:25.500
                Some of their questions unanswered. We found getting information just as challenging.""";
        Map<String, String> aggregate = new HashMap<>();
        aggregate.put(OpenAI.AudioResponseFormat.JSON.getValue(), jsonResponse);
        aggregate.put(OpenAI.AudioResponseFormat.TEXT.getValue(), textResponse);
        aggregate.put(OpenAI.AudioResponseFormat.SUB_RIP_TEXT.getValue(), srtResponse);
        aggregate.put(OpenAI.AudioResponseFormat.VERBOSE_JSON.getValue(), verboseJsonResponse);
        aggregate.put(OpenAI.AudioResponseFormat.VIDEO_TEXT_TRACKS.getValue(), vttResponse);

        OpenAI.TranslationRequest request = new OpenAI.TranslationRequest(new FileInputStream("src/test/resources/openai/cnn230223n_1658968adw.mp3"), "cnn230223n_1658968adw.mp3", "whisper-1");
        request.setResponseFormat(OpenAI.AudioResponseFormat.getInstance(responseFormat));

        MultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<>(headers);
        if (!responseFormat.equals(OpenAI.AudioResponseFormat.JSON.getValue()) && !responseFormat.equals(OpenAI.AudioResponseFormat.VERBOSE_JSON.getValue())) {
            responseHeaders.put("Content-Type", List.of("text/plain;charset=utf-8"));
        }

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(responseHeaders));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(aggregate.get(responseFormat).getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.TranscriptionResponse result = openAI.translate(request).get();
        Assertions.assertTrue(result.toString().contains("carrying toxic chemicals"));
    }

    @Test
    public void testUploadFineTuneFile() throws Exception {
        String response = """
                {
                  "object": "file",
                  "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                  "purpose": "fine-tune",
                  "filename": "1677156793626.jsonl",
                  "bytes": 281,
                  "created_at": 1676983926,
                  "status": "uploaded",
                  "status_details": null
                }""";
        InputStream is = new ByteArrayInputStream("{\"prompt\":\"什么是美式双标\",\"completion\":\"批评前苏联切尔诺贝利核电站泄露，却对日本福岛核电站排泄核污水认为置若罔闻。\"}\n{\"prompt\":\"什么是美式民主\",\"completion\":\"香港暴乱赞扬是民主，支持川普的民众被指责是暴力\"}".getBytes(StandardCharsets.UTF_8));
        int bytes = is.available();
        String fileName = "1677156793626.jsonl";

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.FineTuneFile result = openAI.uploadFineTuneFile(is, fileName, "fine-tune").join();
        Assertions.assertEquals("file", result.object());
        Assertions.assertEquals("fine-tune", result.purpose());
        Assertions.assertEquals(bytes, result.bytes());
        Assertions.assertEquals(fileName, result.fileName());
    }

    @Test
    public void testListFineTuneFiles() {
        String response = """
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "file",
                      "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                      "purpose": "fine-tune",
                      "filename": "1677156793626.jsonl",
                      "bytes": 281,
                      "created_at": 1677156794,
                      "status": "processed",
                      "status_details": null
                    }
                  ]
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.Response<OpenAI.FineTuneFile> result = openAI.listFineTuneFiles().join();
        Assertions.assertEquals("list", result.getObject());
        Assertions.assertTrue(result.getData().size() > 0);
        Assertions.assertEquals("file", result.getData().get(0).object());
        Assertions.assertEquals("fine-tune", result.getData().get(0).purpose());
        Assertions.assertEquals(result.getData().size(), result.getData().stream().filter(d -> d.bytes() > 0 && d.status() != null).count());
    }

    @Test
    public void testRetrieveFineTuneFile() {
        String response = """
                {
                  "object": "file",
                  "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                  "purpose": "fine-tune",
                  "filename": "1677156793626.jsonl",
                  "bytes": 281,
                  "created_at": 1677156794,
                  "status": "processed",
                  "status_details": null
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.FineTuneFile result = openAI.retrieveFineTuneFile("file-KWNcORDLYJ8vBs1qYx0DjCoo").join();
        Assertions.assertEquals("file", result.object());
        Assertions.assertEquals("fine-tune", result.purpose());
        Assertions.assertTrue(result.bytes() > 0 && result.status() != null);
    }

    @Test
    public void testRetrieveFineTuneFileContent() {
        // 免费账户被禁用下载微调文件
    }

    @Test
    public void testDeleteFineTuneFile() {
        String response = """
                {
                  "object": "file",
                  "id": "file-0qgNmk9HQYgFxiVKt9oQ7I2f",
                  "deleted": true
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        String fileId = "file-0qgNmk9HQYgFxiVKt9oQ7I2f";
        OpenAI.DeleteResponse result = openAI.deleteFineTuneFile(fileId).join();
        Assertions.assertEquals("file", result.object());
        Assertions.assertEquals(fileId, result.id());
        Assertions.assertTrue(result.deleted());
    }

    @Test
    public void testCreateFineTune() {
        String response = """
                {
                  "object": "fine-tune",
                  "id": "ft-FaEJWiZyUyhJ2vvAP6l3EiGJ",
                  "hyperparams": {
                    "n_epochs": 4,
                    "batch_size": null,
                    "prompt_loss_weight": 0.01,
                    "learning_rate_multiplier": null
                  },
                  "organization_id": "org-scLMFVYrYyin6oFpv38dnCGr",
                  "model": "curie",
                  "training_files": [
                    {
                      "object": "file",
                      "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                      "purpose": "fine-tune",
                      "filename": "1677156793626.jsonl",
                      "bytes": 281,
                      "created_at": 1677156794,
                      "status": "processed",
                      "status_details": null
                    }
                  ],
                  "validation_files": [],
                  "result_files": [],
                  "created_at": 1677157578,
                  "updated_at": 1677157578,
                  "status": "pending",
                  "fine_tuned_model": null,
                  "events": [
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Created fine-tune: ft-FaEJWiZyUyhJ2vvAP6l3EiGJ",
                      "created_at": 1677157578
                    }
                  ]
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.FineTuneRequest request = OpenAI.FineTuneRequest.withDefault("file-KWNcORDLYJ8vBs1qYx0DjCoo");
        OpenAI.FineTuneResponse result = openAI.fineTune(request).join();

        Assertions.assertEquals("fine-tune", result.object());
        if (request.model() != null) {
            Assertions.assertEquals(request.model(), result.model());
        } else {
            Assertions.assertEquals("curie", result.model());
        }
        Assertions.assertTrue(result.id() != null && result.status() != null);
        Assertions.assertTrue(result.trainingFiles().stream().anyMatch(f -> f.id().equals(request.trainingFile())));
    }

    @Test
    public void testListFineTunes() {
        String response = """
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "fine-tune",
                      "id": "ft-jRE7h8HJS1HlohGGAruiZ5jY",
                      "hyperparams": {
                        "n_epochs": 4,
                        "batch_size": 1,
                        "prompt_loss_weight": 0.01,
                        "learning_rate_multiplier": 0.1
                      },
                      "organization_id": "org-scLMFVYrYyin6oFpv38dnCGr",
                      "model": "curie",
                      "training_files": [
                        {
                          "object": "file",
                          "id": "file-ZrLfgcDm8zAG89tZgvuF6fBY",
                          "purpose": "fine-tune",
                          "filename": "lujincaifu.json",
                          "bytes": 508,
                          "created_at": 1677073529,
                          "status": "processed",
                          "status_details": null
                        }
                      ],
                      "validation_files": [],
                      "result_files": [
                        {
                          "object": "file",
                          "id": "file-Vi90Msrp0TS41OTchNFPJQo2",
                          "purpose": "fine-tune-results",
                          "filename": "compiled_results.csv",
                          "bytes": 555,
                          "created_at": 1677074421,
                          "status": "processed",
                          "status_details": null
                        }
                      ],
                      "created_at": 1677073646,
                      "updated_at": 1677074421,
                      "status": "succeeded",
                      "fine_tuned_model": "curie:ft-personal-2023-02-22-14-00-19"
                    },
                    {
                      "object": "fine-tune",
                      "id": "ft-FaEJWiZyUyhJ2vvAP6l3EiGJ",
                      "hyperparams": {
                        "n_epochs": 4,
                        "batch_size": 1,
                        "prompt_loss_weight": 0.01,
                        "learning_rate_multiplier": 0.1
                      },
                      "organization_id": "org-scLMFVYrYyin6oFpv38dnCGr",
                      "model": "curie",
                      "training_files": [
                        {
                          "object": "file",
                          "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                          "purpose": "fine-tune",
                          "filename": "1677156793626.jsonl",
                          "bytes": 281,
                          "created_at": 1677156794,
                          "status": "processed",
                          "status_details": null
                        }
                      ],
                      "validation_files": [],
                      "result_files": [
                        {
                          "object": "file",
                          "id": "file-0qgNmk9HQYgFxiVKt9oQ7I2f",
                          "purpose": "fine-tune-results",
                          "filename": "compiled_results.csv",
                          "bytes": 530,
                          "created_at": 1677158294,
                          "status": "deleted",
                          "status_details": null
                        }
                      ],
                      "created_at": 1677157578,
                      "updated_at": 1677158294,
                      "status": "succeeded",
                      "fine_tuned_model": "curie:ft-personal-2023-02-23-13-18-13"
                    },
                    {
                      "object": "fine-tune",
                      "id": "ft-28iqSpUbsf4kpx0GYEfT8w2p",
                      "hyperparams": {
                        "n_epochs": 4,
                        "batch_size": 1,
                        "prompt_loss_weight": 0.01,
                        "learning_rate_multiplier": 0.1
                      },
                      "organization_id": "org-scLMFVYrYyin6oFpv38dnCGr",
                      "model": "curie",
                      "training_files": [
                        {
                          "object": "file",
                          "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                          "purpose": "fine-tune",
                          "filename": "1677156793626.jsonl",
                          "bytes": 281,
                          "created_at": 1677156794,
                          "status": "processed",
                          "status_details": null
                        }
                      ],
                      "validation_files": [],
                      "result_files": [
                        {
                          "object": "file",
                          "id": "file-1uF9uJ4Bs5T8ZnzyjxMDI659",
                          "purpose": "fine-tune-results",
                          "filename": "compiled_results.csv",
                          "bytes": 534,
                          "created_at": 1677158658,
                          "status": "processed",
                          "status_details": null
                        }
                      ],
                      "created_at": 1677158147,
                      "updated_at": 1677158659,
                      "status": "succeeded",
                      "fine_tuned_model": "curie:ft-personal-2023-02-23-13-24-18"
                    }
                  ]
                }""";

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.Response<OpenAI.FineTuneResponse> result = openAI.listFineTunes().join();
        Assertions.assertEquals("list", result.getObject());
        Assertions.assertTrue(result.getData().size() > 0);
        Assertions.assertEquals(result.getData().size(), result.getData().stream().filter(ft -> "fine-tune".equals(ft.object())).count());
    }

    @Test
    public void testRetrieveFineTune() {
        String response = """
                {
                  "object": "fine-tune",
                  "id": "ft-28iqSpUbsf4kpx0GYEfT8w2p",
                  "hyperparams": {
                    "n_epochs": 4,
                    "batch_size": 1,
                    "prompt_loss_weight": 0.01,
                    "learning_rate_multiplier": 0.1
                  },
                  "organization_id": "org-scLMFVYrYyin6oFpv38dnCGr",
                  "model": "curie",
                  "training_files": [
                    {
                      "object": "file",
                      "id": "file-KWNcORDLYJ8vBs1qYx0DjCoo",
                      "purpose": "fine-tune",
                      "filename": "1677156793626.jsonl",
                      "bytes": 281,
                      "created_at": 1677156794,
                      "status": "processed",
                      "status_details": null
                    }
                  ],
                  "validation_files": [],
                  "result_files": [
                    {
                      "object": "file",
                      "id": "file-1uF9uJ4Bs5T8ZnzyjxMDI659",
                      "purpose": "fine-tune-results",
                      "filename": "compiled_results.csv",
                      "bytes": 534,
                      "created_at": 1677158658,
                      "status": "processed",
                      "status_details": null
                    }
                  ],
                  "created_at": 1677158147,
                  "updated_at": 1677158659,
                  "status": "succeeded",
                  "fine_tuned_model": "curie:ft-personal-2023-02-23-13-24-18",
                  "events": [
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Created fine-tune: ft-28iqSpUbsf4kpx0GYEfT8w2p",
                      "created_at": 1677158147
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune costs $0.00",
                      "created_at": 1677158565
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune enqueued. Queue number: 0",
                      "created_at": 1677158565
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune started",
                      "created_at": 1677158576
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 1/4",
                      "created_at": 1677158636
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 2/4",
                      "created_at": 1677158636
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 3/4",
                      "created_at": 1677158637
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 4/4",
                      "created_at": 1677158637
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Uploaded model: curie:ft-personal-2023-02-23-13-24-18",
                      "created_at": 1677158658
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Uploaded result file: file-1uF9uJ4Bs5T8ZnzyjxMDI659",
                      "created_at": 1677158658
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune succeeded",
                      "created_at": 1677158659
                    }
                  ]
                }""";

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        OpenAI.FineTuneResponse result = openAI.retrieveFineTune("ft-28iqSpUbsf4kpx0GYEfT8w2p").join();
        Assertions.assertNotNull(result.id());
        Assertions.assertEquals("fine-tune", result.object());
        Assertions.assertTrue(result.trainingFiles().size() > 0);
        Assertions.assertTrue(result.events().size() > 0);
    }

    @Test
    public void testCancelFineTune() {
        // if create fine tune success, got 400 error
        String error = """
                {
                  "error": {
                    "message": "Cannot cancel a job ft-28iqSpUbsf4kpx0GYEfT8w2p that already has status \\"succeeded\\".",
                    "type": "invalid_request_error",
                    "param": null,
                    "code": null
                  }
                }""";

        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(error.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        Assertions.assertThrows(CompletionException.class, () -> openAI.cancelFineTune("ft-28iqSpUbsf4kpx0GYEfT8w2p").join());
    }

    @Test
    public void testListFineTuneEvents() {
        String response = """
                {
                  "object": "list",
                  "data": [
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Created fine-tune: ft-28iqSpUbsf4kpx0GYEfT8w2p",
                      "created_at": 1677158147
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune costs $0.00",
                      "created_at": 1677158565
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune enqueued. Queue number: 0",
                      "created_at": 1677158565
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune started",
                      "created_at": 1677158576
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 1/4",
                      "created_at": 1677158636
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 2/4",
                      "created_at": 1677158636
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 3/4",
                      "created_at": 1677158637
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Completed epoch 4/4",
                      "created_at": 1677158637
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Uploaded model: curie:ft-personal-2023-02-23-13-24-18",
                      "created_at": 1677158658
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Uploaded result file: file-1uF9uJ4Bs5T8ZnzyjxMDI659",
                      "created_at": 1677158658
                    },
                    {
                      "object": "fine-tune-event",
                      "level": "info",
                      "message": "Fine-tune succeeded",
                      "created_at": 1677158659
                    }
                  ]
                }
                """;
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.Response<OpenAI.FineTuneEvent> result = openAI.listFineTuneEvents("ft-28iqSpUbsf4kpx0GYEfT8w2p", false).join();
        Assertions.assertEquals("list", result.getObject());
        Assertions.assertTrue(result.getData().size() > 0);
        Assertions.assertEquals(result.getData().size(), result.getData().stream().filter(e -> "fine-tune-event".equals(e.object())).count());
    }

    @Test
    public void testDeleteFineTuneModel() {
        String response = """
                {
                  "id": "curie:ft-personal-2023-02-23-13-18-13",
                  "object": "model",
                  "deleted": true
                }""";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));

        String model = "curie:ft-personal-2023-02-23-13-18-13";
        OpenAI.DeleteResponse result = openAI.deleteFineTuneModel(model).join();

        Assertions.assertEquals(model, result.id());
        Assertions.assertEquals("model", result.object());
        Assertions.assertTrue(result.deleted());
        // if model not exist, got 404
    }

    @Test
    public void testModerations() {
        String response = "{\"id\":\"modr-6n76Y8pDYarMVtFMCd9sy8ZRRa4AH\",\"model\":\"text-moderation-004\",\"results\":[{\"flagged\":false,\"categories\":{\"sexual\":false,\"hate\":false,\"violence\":false,\"self-harm\":false,\"sexual/minors\":false,\"hate/threatening\":false,\"violence/graphic\":false},\"category_scores\":{\"sexual\":0.00011616510164458305,\"hate\":1.0768698302854318e-05,\"violence\":0.009578769095242023,\"self-harm\":0.0001046927209245041,\"sexual/minors\":3.5266660347588186e-07,\"hate/threatening\":2.3478714528124556e-09,\"violence/graphic\":1.6347416931239422e-06}}]}";
        Mockito.when(clientHttpResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(clientHttpResponse.getHeaders()).thenReturn(HttpHeaders.readOnlyHttpHeaders(headers));
        Mockito.when(clientHttpResponse.getBody()).thenReturn(DataBufferUtils.readInputStream(() ->new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)), DefaultDataBufferFactory.sharedInstance, 8096));
        Mockito.when(clientHttpConnector.connect(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(clientHttpResponse));
        OpenAI.ModerationResponse result = openAI.moderations("killing spree").join();
        Assertions.assertNotNull(result.id());
        Assertions.assertEquals("text-moderation-004", result.model());
        Assertions.assertTrue(result.results().size() > 0);
        Assertions.assertFalse(result.results().get(0).flagged());

    }

}
