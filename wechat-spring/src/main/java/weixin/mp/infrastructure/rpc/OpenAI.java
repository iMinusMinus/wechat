package weixin.mp.infrastructure.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <a href="https://platform.openai.com/docs/api-reference/introduction">OpenAI API</a>
 */
public interface OpenAI {

    // 1. models
    /**
     * 列出当前可用模型

     * @return 可用模型清单
     */
    CompletableFuture<Response<ModelResponse>> listModel();

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Response<T> {

        protected List<T> data;

        protected String object;

        public List<T> getData() {
            return data;
        }

        public void setData(List<T> data) {
            this.data = data;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "data=" + data +
                    ", object='" + object + '\'' +
                    '}';
        }
    }

    /**
     * 获取模型信息
     * @param model 模型id
     * @return 模型基本信息
     */
    CompletableFuture<ModelResponse> retrieveModel(@NotNull String model);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModelResponse(@JsonProperty("id") String id, String object, Long created,
                         @JsonProperty("owned_by") String owner,
                         @JsonProperty("permission") List<Permission> permission,
                         String root, String parent) {}

    record Permission(String id,
                      String object,
                      Long created,
                      @JsonProperty("allow_create_engine") Boolean allowCreateEngine,
                      @JsonProperty("allow_sampling") Boolean allowSampling,
                      @JsonProperty("allow_logprobs") Boolean allowLogProbabilities,
                      @JsonProperty("allow_search_indices") Boolean allowSearchIndices,
                      @JsonProperty("allow_view") Boolean allowView,
                      @JsonProperty("allow_fine_tuning") Boolean allowFineTuning,
                      String organization,
                      String group,
                      @JsonProperty("is_blocking") Boolean blocking
                      ) {}

    // 2. completions
    /**
     * 给出提示，模型返回至少1个预期的填空/回答
     * @param request 提示等信息
     * @return 生成的回答
     */
    CompletableFuture<TextResponse> complete(@Valid CompletionRequest request);

    /**
     * @param model <ul>模型id
     *              <li>ada(Parsing text, simple classification, address correction, keywords)</li>
     *              <li>babbage(Moderate classification, semantic search classification)</li>
     *              <li>curie(Language translation, complex classification, text sentiment, summarization)</li>
     *              <li>davinci(Complex intent, cause and effect, summarization for audience)</li>
     *              </ul>如text-davinci-003
     * @param prompt 问题/提示
     * @param suffix 完成文本的后缀
     * @param maxTokens 1 token=4 characters ≈ 0.75words，大部分模型不能超过2048 token
     * @param temperature 用于控制随机性，0.8以上输出内容更随机，0.2以下更确定
     * @param topProbability 靠前概率的内容将被选择。另一种调节输出内容的参数，与temperature不同时使用
     * @param n 每次请求输出N个结果。此参数可能快速消耗token配额，注意设置max_tokens 和 stop
     * @param stream 是否以Server Sent Event形式传输(流以"data: [DONE]"结束)
     * @param logProbabilities 返回包括多个最可能的token
     * @param echo 在输出内容回显提示
     * @param stop
     * @param presencePenalty 正值使得模型偏向于产生新主题
     * @param frequencyPenalty 正值降低模型重复同一行的可能性
     * @param bestOf 生成最佳的内容。best_of不能小于n，该参数指定后不能再设置stream为true
     * @param logitBias 映射token和对应的偏向值(-100~100)
     * @param user OpenAI用于监控是否滥用
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record CompletionRequest(@NotNull String model, String prompt, String suffix,
                             @Size(max=2048) @JsonProperty("max_tokens") Integer maxTokens,
                             @DecimalMax("2.0") @DecimalMin("0.0") @NumberFormat(pattern = "#.#") Float temperature,
                             @DecimalMax("1.0") @DecimalMin("0.0") @JsonProperty("top_p") @NumberFormat(pattern = "#.#") Float topProbability,
                             Integer n,
                             Boolean stream,
                             @Max(5) @JsonProperty("logprobs") Integer logProbabilities,
                             Boolean echo,
                             @Size(max = 4) String[] stop,
                             @DecimalMax("2.0") @DecimalMin("-2.0") @JsonProperty("frequency_penalty") @NumberFormat(pattern = "#.#") Float presencePenalty,
                             @DecimalMax("2.0") @DecimalMin("-2.0") @JsonProperty("presence_penalty") @NumberFormat(pattern = "#.#") Float frequencyPenalty,
                             @JsonProperty("best_of") Integer bestOf,
                             @JsonProperty("logit_bias") Map<String, @Max(100) @Min(-100) Integer> logitBias,
                             String user) {
        public static CompletionRequest withDefault(String model) {
            return new CompletionRequest(model, "<|endoftext|>", null, 16, 1f,
                    1f, 1, false, null, false, null, 0.0f,
                    0.0f, 1, null, null);
        }

        public static CompletionRequest of(String model, String prompt, int maxTokens, String[] stop) {
            return new CompletionRequest(model, prompt, null, maxTokens, null,
                    null, null, null, null, null, stop, null,
                    null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TextResponse(String id, String object, long created, String model, List<Choice> choices, Usage usage) {}

    /**
     * @param text
     * @param index
     * @param logProbabilities logprobs
     * @param finishReason finish_reason
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(String text, Integer index, @JsonProperty("logprobs") Integer logProbabilities,
                  @JsonProperty("finish_reason") String finishReason) {

    }

    /**
     * @param promptTokens 提问消耗的token数量
     * @param completionTokens ChatGPT回答消耗的token数量
     * @param totalTokens 消耗的总token量
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                 @JsonProperty("completion_tokens") Integer completionTokens,
                 @JsonProperty("total_tokens") Integer totalTokens) {

    }


    // 3. chat
    /**
     * ChatGPT
     * @param request 对话请求
     * @return AI生成结果
     */
    CompletableFuture<ChatResponse> chat(@Valid ChatRequest request);

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ChatRequest(@NotNull @Pattern(regexp = "^gpt-3.5-turbo(-0301)?$") String model,
                       @NotNull @Size(min = 1) List<ChatMessage> messages,
                       @DecimalMax("2.0") @DecimalMin("0.0") @NumberFormat(pattern = "#.#") Float temperature,
                       @DecimalMax("1.0") @DecimalMin("0.0") @JsonProperty(value = "top_p", defaultValue = "1") @NumberFormat(pattern = "#.#") Float topProbability,
                       @JsonProperty(value = "n", defaultValue = "1") Integer n,
                       @JsonProperty(value = "stream", defaultValue = "false") Boolean stream,
                       @Size(max = 4) String[] stop,
                       @Size(max=4096) @JsonProperty("max_tokens") Integer maxTokens,
                       @DecimalMax("2.0") @DecimalMin("-2.0") @JsonProperty("frequency_penalty") @NumberFormat(pattern = "#.#") Float presencePenalty,
                       @DecimalMax("2.0") @DecimalMin("-2.0") @JsonProperty("presence_penalty") @NumberFormat(pattern = "#.#") Float frequencyPenalty,
                       @JsonProperty("logit_bias") Map<String, @Max(100) @Min(-100) Integer> logitBias,
                       String user) {
        public static ChatRequest withDefault(String model, List<ChatMessage> messages) {
            return new ChatRequest(model, messages, null, null, null, null, null, null, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ChatMessage(Role role, String content) {}

    enum Role {
        USER("user"),
        AI("assistant"),
        ;
        private final String value;

        Role(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String id, String object, long created, List<ChatChoice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatChoice(int index, ChatMessage message, @JsonProperty("finish_reason") String finishReason) {}

    // 4. edits
    /**
     * 给出提示和指令，模型对输入的提示进行修改
     * @param request 原始文本及提示等信息
     * @return 修改后的文本
     */
    CompletableFuture<TextResponse> edit(@Valid EditTextRequest request);

    /**
     * @param model 模型id
     * @param input 需要修改的输入文本
     * @param instruction 如何修改的指令
     * @param n 返回的数量
     * @param temperature 0.8以上结果更随机，0.2以下更聚焦
     * @param topProbability 结果按概率筛选
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record EditTextRequest(@NotNull @Pattern(regexp = "(^text-davinci-edit-001$)|(^code-davinci-edit-001$)") String model,
                           String input,
                           @NotNull String instruction,
                           Integer n,
                           @DecimalMax("2.0") @DecimalMin("0.0") @NumberFormat(pattern = "#.#") Float temperature,
                           @JsonProperty("top_p") Float topProbability) {
        public static EditTextRequest withDefault(String model, String instruction) {
            return new EditTextRequest(model, "", instruction, 1, 1f, 1f);
        }

        public static EditTextRequest of(String model, String input, String instruction) {
            return new EditTextRequest(model, input, instruction, 1, 1f, 1f);
        }
    }

    // 5. images
    /**
     * 给出图片的提示，模型产生新图片
     * @param request 图片提示、输出格式等
     * @return 图片URL或base64编码的图片
     */
    CompletableFuture<ImageResponse> generate(@Valid ImageGenerationRequest request);

    /**
     * 根据文本描述生成图片
     * @param prompt 描述图像内容的文本
     * @param n 生成图片数量
     * @param size 生成的图片大小
     * @param responseFormat 指定返回图片的输出方式
     * @param user 用户标识
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record ImageGenerationRequest(@NotNull @Size(max = 1000) String prompt,
                                  @Min(1) @Max(10) Integer n,
                                  ImageSize size,
                                  @JsonProperty("response_format") ImageResponseFormat responseFormat,
                                  String user) {
        public static ImageGenerationRequest withDefault(String prompt) {
            return new ImageGenerationRequest(prompt, 1, ImageSize.LARGE, ImageResponseFormat.URL, null);
        }
    }

    enum ImageSize {
        SMALL(256, 256),
        MEDIUM(512, 512),
        LARGE(1024, 1024),
        ;

        private final int width;

        private final int height;

        private ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @JsonValue
        public String description() {
            return width + "x" + height;
        }
    }

    enum ImageResponseFormat {
        URL("url"),
        BASE64_JSON("b64_json"),
        ;
        private final String value;

        private ImageResponseFormat(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageResponse(Long created, List<ImageRepresentation> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ImageRepresentation(@JsonProperty("url") String url, @JsonProperty("b64_json") String base64EncodedImage) {}

    /**
     * 根据图片和提示，模型对图片进行修改
     * @param request 原图片及提示等信息
     * @return 修改后的图片
     */
    CompletableFuture<ImageResponse> edit(@Valid EditImageRequest request);

    /**
     * @param image 需要编辑的图片（必须是PNG格式，为正方形，小于4M）。如果蒙蒙版mask)未指定，该图片必须有透明度(alpha)
     * @param mask 蒙版图片（必须是PNG格式，小于4M，和要编辑的图片大小相同，且全透明，即alpha为0）
     * @param prompt 期望图片的描述
     * @param n 生成图片的个数
     * @param size 生成图片的大小
     * @param responseFormat 生成图片的格式，即图片url还是base64编码的图片
     * @param user 用户
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record EditImageRequest(@NotNull InputStream image,
                            @NotNull String imageFileName,
                            InputStream mask,
                            String maskFileName,
                            @NotNull @Size(max = 1000) String prompt,
                            @Min(1) @Max(10) Integer n,
                            ImageSize size,
                            @JsonProperty("response_format") ImageResponseFormat responseFormat,
                            String user) {
        public static EditImageRequest withDefault(InputStream image, String imageFileName, String prompt) {
            return new EditImageRequest(image, imageFileName, null, null, prompt, 1, ImageSize.LARGE, ImageResponseFormat.URL, null);
        }
    }

    /**
     * 根据现有图片生成变种图片
     * @param request 原始图片等信息
     * @return 变种图片
     */
    CompletableFuture<ImageResponse> mutate(@Valid VariantImageRequest request);

    /**
     * @param image 需变换的图片（需为PNG图片，正方形，小于4M）
     * @param n 生成图片数量
     * @param size 图片大小
     * @param responseFormat 返回图片url还是base64编码图片
     * @param user 用户
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record VariantImageRequest(@NotNull InputStream image,
                               @NotNull String imageFileName,
                               @Min(1) @Max(10) Integer n,
                               ImageSize size,
                               @JsonProperty("response_format") ImageResponseFormat responseFormat,
                               String user) {
        public static VariantImageRequest withDefault(InputStream image, String fileName) {
            return new VariantImageRequest(image, fileName, 1, ImageSize.LARGE, ImageResponseFormat.URL, null);
        }
    }

    // 6. embeddings
    /**
     * 将输入的词转换为向量（用于给机器学习模型或算法）
     * @param request 文本及模型信息
     * @return 文本的向量表示
     */
    CompletableFuture<EmbeddingResponse> embedding(@Valid EmbeddingRequest request);

    /**
     * @param model 模型id
     * @param input 需要转换为向量的词句
     * @param user 用户
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record EmbeddingRequest(@NotNull String model, @NotNull @Size(max = 8192) String input, String user) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(String object, List<EmbeddingResult> data, String model, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResult(String object, float[] embedding, Integer index) {}

    // 7. audio

    /**
     * 将音频内容转录成文本
     * @param request
     * @return 音频内容对应的文字
     */
    CompletableFuture<? extends TranscriptionResponse> transcribe(@Valid TranscribeRequest request);

    /**
     * 将音频内容翻译成英文
     * @param request
     * @return 音频内容对应的英文
     */
    CompletableFuture<? extends TranscriptionResponse> translate(@Valid TranslationRequest request);

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    class TranslationRequest {
        public TranslationRequest(InputStream file, String fileName, String model) {
            this.file = file;
            this.fileName = fileName;
            this.model = model;
        }

        /**
         * 音频，格式必须是其中之一： mp3, mp4, mpeg, mpga, m4a, wav, webm。
         * 文件大小不超过25MB。
         */
        @NotNull
        protected final InputStream file;

        /**
         * 文件名
         */
        @NotNull
        protected final String fileName;

        /**
         * 模型id，当前仅支持"whisper-1"
         */
        @NotNull
        protected final String model;
        /**
         * 指导模型风格或继续上一个音频，必须是英文
         */
        protected String prompt;
        /**
         * 转译脚本格式
         */
        @JsonProperty(value = "response_format", defaultValue = "json")
        protected AudioResponseFormat responseFormat;
        @JsonProperty(value = "temperature", defaultValue = "0")
        protected Float temperature;

        public InputStream getFile() {
            return file;
        }

        public String getFileName() {
            return fileName;
        }

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public AudioResponseFormat getResponseFormat() {
            return responseFormat;
        }

        public void setResponseFormat(AudioResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
        }

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }
    }

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    class TranscribeRequest extends TranslationRequest {
        public TranscribeRequest(InputStream file, String fileName, String model) {
            super(file, fileName, model);
        }

        /**
         * 输入音频的语言，必须符合 ISO-639-1 规范。提供此参数可以获得更好的准确率，并降低延迟
         */
        private String language;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    enum AudioResponseFormat {
        JSON("json"),
        TEXT("text"),
        SUB_RIP_TEXT("srt"),
        VERBOSE_JSON("verbose_json"),
        VIDEO_TEXT_TRACKS("vtt"),
        ;

        private final String value;

        AudioResponseFormat(String value) {
            this.value = value;
        }

        static AudioResponseFormat getInstance(String value) {
            for (AudioResponseFormat instance : AudioResponseFormat.values()) {
                if (instance.value.equals(value)) {
                    return instance;
                }
            }
            return null;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    interface TranscriptionResponse {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscriptionJsonResponse(String text) implements TranscriptionResponse {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscriptionSubRipTextResponse(List<AudioSegment> segments) implements TranscriptionResponse {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
        static TranscriptionSubRipTextResponse parse(String body) {
            String[] parts = body.split("\n");
            List<AudioSegment> segments = new ArrayList<>(parts.length / 4);
            for (int i = 0; i < parts.length; i += 4) {
                String[] track = parts[i + 1].split(" --> ");
                segments.add(new AudioSegment(Integer.parseInt(parts[i]), LocalTime.parse(track[0], FMT), LocalTime.parse(track[1], FMT), parts[i + 2]));
            }
            return new TranscriptionSubRipTextResponse(segments);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (AudioSegment segment : segments) {
                sb.append(segment.text());
            }
            return sb.toString();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record  TranscriptionVerboseJsonResponse(String task, String language, float duration, List<TranscribedAudioSegment> segments, String text) implements TranscriptionResponse {
        @Override
        public String toString() {
            return text;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscribedAudioSegment(int id, int seek,
                                   float start, float end, String text,
                                   int[] tokens,
                                   float temperature,
                                   @JsonProperty("avg_logprob") float avgLogProbability,
                                   @JsonProperty("compression_ratio") float compressionRatio,
                                   @JsonProperty("no_speech_prob") float noSpeechProbability,
                                   @JsonProperty("transient") boolean temporal) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscriptionVideoTextTracksResponse(String type, List<AudioSegment> segments) implements TranscriptionResponse {
        static TranscriptionVideoTextTracksResponse parse(String body) {
            String[] parts = body.split("\n");
            String type = parts[0];
            List<AudioSegment> segments = new ArrayList<>((parts.length - 1) / 3);
            for (int i = 2; i < parts.length; i += 3) {
                String[] track = parts[i].split(" --> ");
                segments.add(new AudioSegment((i + 1) / 3, LocalTime.parse(track[0]), LocalTime.parse(track[1]), parts[i + 1]));
            }
            return new TranscriptionVideoTextTracksResponse(type, segments);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (AudioSegment segment : segments) {
                sb.append(segment.text());
            }
            return sb.toString();
        }
    }

    record AudioSegment(int sequence, LocalTime start, LocalTime end, String text) {}

    // 8. files
    /**
     * 列举微调文件
     * @return 微调文件存储清单
     */
    CompletableFuture<Response<FineTuneFile>> listFineTuneFiles();

    /**
     * 上传微调文件
     * @param file JSON文件，每一行由prompt和completion字段构成。一个组织的所有微调文件不能大于1GB
     * @param purpose 上传文档目的，如: "fine-tune"
     * @return 文件存储结果信息
     */
    CompletableFuture<FineTuneFile> uploadFineTuneFile(@NotNull InputStream file, @NotNull String fileName, @NotNull String purpose);

    /**
     * 获取微调文件信息，<b>免费账号被禁止下载微调文件</b>
     * @param fileId 上传后返回的id
     * @return 文件存储信息
     */
    CompletableFuture<FineTuneFile> retrieveFineTuneFile(String fileId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FineTuneFile(String id, String object, int bytes, @JsonProperty("created_at") long createdAt,
                        @JsonProperty("filename") String fileName, String purpose, String status) {}

    /**
     * 删除文件
     * @param fileId 文件id（上传后返回的id）
     * @return 删除结果
     */
    CompletableFuture<DeleteResponse> deleteFineTuneFile(@NotNull String fileId);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeleteResponse(String id, String object, boolean deleted) {}

    // 9. fine-tunes 当前个人账号大部分模型没有权限调优

    /**
     * 创建一个定时任务，用于使用给定数据集训练模型
     * @param request 训练模型及数据集、参数等信息
     * @return 任务信息
     */
    CompletableFuture<FineTuneResponse> fineTune(@Valid FineTuneRequest request);

    /**
     * @param trainingFile 上传的fine-tune文件返回id
     * @param validationFile 上传的验证数据文件返回的id
     * @param model 需要微调的基础模型，模型需在2022-04-21后创建
     * @param epochs 在训练集上训练模型几个周期
     * @param batchSize 训练时使用训练集数据量
     * @param learningRateMultiplier
     * @param promptLossWeight
     * @param computeClassificationMetrics
     * @param classificationClassesQuantity
     * @param classificationPositiveClass
     * @param classificationBetas
     * @param suffix
     */
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    record FineTuneRequest(@NotNull @JsonProperty("training_file") String trainingFile,
                           @JsonProperty("validation_file") String validationFile,
                           @JsonProperty(value = "model", defaultValue = "curie") String model,
                           @JsonProperty(value = "n_epochs", defaultValue = "4") Integer epochs,
                           @JsonProperty("batch_size") Integer batchSize,
                           @DecimalMax("0.2") @DecimalMin("0.02") @JsonProperty("learning_rate_multiplier") Float learningRateMultiplier,
                           @JsonProperty(value = "prompt_loss_weight", defaultValue = "0.01") Float promptLossWeight,
                           @JsonProperty(value = "compute_classification_metrics", defaultValue = "false") Boolean computeClassificationMetrics,
                           @JsonProperty("classification_n_classes") Integer classificationClassesQuantity,
                           @JsonProperty("classification_positive_class") String classificationPositiveClass,
                           @JsonProperty("classification_betas") int[] classificationBetas,
                           @Size(max = 40) String suffix) {
        public static FineTuneRequest withDefault(String trainingFile) {
            return new FineTuneRequest(trainingFile, null, "curie", 4, null, null, 0.01f, false, null, null, null, null);
        }
    }

    /**
     * 列出组织微调任务
     * @return 微调任务清单
     */
    CompletableFuture<Response<FineTuneResponse>> listFineTunes();

    /**
     * 获取指定微调任务信息
     * @param fineTuneId 创建微调任务返回的id
     * @return 微调任务信息
     */
    CompletableFuture<FineTuneResponse> retrieveFineTune(@NotNull String fineTuneId);

    /**
     * 取消微调任务
     * @param fineTuneId 创建微调任务返回的id
     * @return 任务信息
     */
    CompletableFuture<FineTuneResponse> cancelFineTune(@NotNull String fineTuneId);

    /**
     * 列举微调任务事件
     * @param fineTuneId 创建微调任务返回的id
     * @param stream 是否以流形式(SSE)返回
     * @return 微调任务事件
     */
    CompletableFuture<Response<FineTuneEvent>> listFineTuneEvents(@NotNull String fineTuneId, Boolean stream);

    /**
     * 删除微调后的模型，调用者必须是组织的所有者
     * @param model 模型id
     * @return 删除结果
     */
    CompletableFuture<DeleteResponse> deleteFineTuneModel(@NotNull String model);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FineTuneResponse(String id,
                            String object,
                            String model,
                            @JsonProperty("created_at") Long createdAt,
                            List<FineTuneEvent> events,
                            @JsonProperty("fine_tuned_model") Object fineTunedModel,
                            @JsonProperty("hyperparams") FineTuneHyperParams hyperParams,
                            @JsonProperty("organization_id") String organizationId,
                            @JsonProperty("result_files") List<Object> resultFiles,
                            String status,
                            @JsonProperty("validation_files") List<Object> validationFiles,
                            @JsonProperty("training_files") List<FineTuneFile> trainingFiles,
                            @JsonProperty("updated_at") Long updatedAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FineTuneEvent(String object, @JsonProperty("created_at") Long createdAt, String level, String message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FineTuneHyperParams(@JsonProperty("batch_size") Integer batchSize,
                               @JsonProperty("learning_rate_multiplier") Float learningRateMultiplier,
                               @JsonProperty("n_epochs") Integer epochs,
                               @JsonProperty("prompt_loss_weight") Float promptLossWeight) {}


    // 10. moderations
    /**
     * 对文本分类，检查是否违反内容审查策略
     * @param input 文本
     * @return 是否有违反策略，及个策略的分值
     */
    CompletableFuture<ModerationResponse> moderations(@NotNull String input);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModerationResponse(String id, String model, List<ModerationResult> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ModerationResult(Map<String, Boolean> categories,
                            @JsonProperty("category_scores") Map<String, Float> categoryScores,
                            Boolean flagged) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorResponse(ErrorResult error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorResult(String message, String type, Object param, Object code) {}

    class JavaClient implements OpenAI {

        private static final String BASE_URL = "https://api.openai.com";

        private static final int BUFFER_SIZE = 8192;

        private static final int BUFFER_LIMIT = 4 * 1024 * 1024;

        private static final ParameterizedTypeReference<Response<ModelResponse>> MODEL_LIST_RESPONSE = new ParameterizedTypeReference<>() {
        };

        private static final ParameterizedTypeReference<Response<FineTuneFile>> FILE_LIST_RESPONSE = new ParameterizedTypeReference<>() {
        };

        private static final ParameterizedTypeReference<Response<FineTuneResponse>> FINE_TUNE_LIST_RESPONSE = new ParameterizedTypeReference<>() {
        };

        private static final ParameterizedTypeReference<Response<FineTuneEvent>> FINE_TUNE_EVENT_LIST_RESPONSE = new ParameterizedTypeReference<>() {
        };

        private final WebClient webClient;

        public JavaClient(String bearerToken) {
            this.webClient = WebClient.builder().baseUrl(BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + bearerToken)
                    // codec默认maxInMemorySize为256K，生成图片等接口如果返回base64编码图片数据，会抛DataBufferLimitException。而图片不大于4MB
                    .exchangeStrategies(ExchangeStrategies.builder().codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(BUFFER_LIMIT)).build())
                    .build();
        }

        /**
         * internal use, especially for mock when running test
         * @param webClient
         */
        JavaClient(WebClient webClient) {
            this.webClient = webClient;
        }

        @Override
        public CompletableFuture<Response<ModelResponse>> listModel() {
            return webClient.get().uri("/v1/models")
                    .retrieve()
                    .bodyToMono(MODEL_LIST_RESPONSE)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ModelResponse> retrieveModel(String model) {
            return webClient.get().uri("/v1/models/{model}", model)
                    .retrieve()
                    .bodyToMono(ModelResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<TextResponse> complete(CompletionRequest request) {
            return webClient.method(HttpMethod.POST).uri("/v1/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TextResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ChatResponse> chat(ChatRequest request) {
            return webClient.method(HttpMethod.POST).uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<TextResponse> edit(EditTextRequest request) {
            return webClient.post().uri("/v1/edits")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(TextResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ImageResponse> generate(ImageGenerationRequest request) {
            return webClient.post().uri("/v1/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ImageResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ImageResponse> edit(EditImageRequest request) {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            // MUST specify file name or 400 bad request
            builder.asyncPart("image", DataBufferUtils.readInputStream(request::image, DefaultDataBufferFactory.sharedInstance, BUFFER_SIZE), DataBuffer.class)
                    .contentType(MediaType.IMAGE_PNG)
                    .filename(request.imageFileName());
            if (request.mask() != null) {
                builder.asyncPart("mask", DataBufferUtils.readInputStream(request::mask, DefaultDataBufferFactory.sharedInstance, BUFFER_SIZE), DataBuffer.class)
                        .contentType(MediaType.IMAGE_PNG)
                        .filename(request.maskFileName());
            }
            builder.part("prompt", request.prompt());
            if (request.n() != null) {
                builder.part("n", request.n());
            }
            if (request.size() != null) {
                builder.part("size", request.size().description());
            }
            if (request.responseFormat() != null) {
                builder.part("response_format", request.responseFormat().getValue());
            }
            if (request.user() != null) {
                builder.part("user", request.user());
            }
            return webClient.post().uri("/v1/images/edits")
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(ImageResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ImageResponse> mutate(VariantImageRequest request) {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            // MUST specify file name or 400 bad request
            builder.asyncPart("image", DataBufferUtils.readInputStream(request::image, DefaultDataBufferFactory.sharedInstance, BUFFER_SIZE), DataBuffer.class)
                    .filename(request.imageFileName())
                    .contentType(MediaType.IMAGE_PNG);
            if (request.n()  != null) {
                builder.part("n", request.n());
            }
            if (request.size()  != null) {
                builder.part("size", request.size().description());
            }
            if (request.responseFormat()  != null) {
                builder.part("response_format", request.responseFormat().getValue());
            }
            if (request.user()  != null) {
                builder.part("user", request.user());
            }
            return webClient.post().uri("/v1/images/variations")
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(ImageResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<EmbeddingResponse> embedding(EmbeddingRequest request) {
            return webClient.post().uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<? extends TranscriptionResponse> transcribe(TranscribeRequest request) {
            MultipartBodyBuilder builder = buildTranslationRequestBody(request);
            if (request.getLanguage() != null) {
                builder.part("language", request.getLanguage());
            }
            Class responseClass = retrieveResponseClass(request.getResponseFormat());
            return webClient.post().uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(responseClass)
                    .map(t -> textToTranscriptionResponse(t, request.getResponseFormat()))
                    .toFuture();
        }

        @Override
        public CompletableFuture<? extends TranscriptionResponse> translate(TranslationRequest request) {
            MultipartBodyBuilder builder = buildTranslationRequestBody(request);
            Class responseClass = retrieveResponseClass(request.getResponseFormat());
            return webClient.post().uri("/v1/audio/translations")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(responseClass)
                    .map(t -> textToTranscriptionResponse(t, request.getResponseFormat()))
                    .toFuture();
        }

        private MultipartBodyBuilder buildTranslationRequestBody(TranslationRequest request) {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.asyncPart("file", DataBufferUtils.readInputStream(() -> request.file, DefaultDataBufferFactory.sharedInstance, BUFFER_SIZE), DataBuffer.class)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .filename(request.getFileName());
            builder.part("model", request.model);
            if (request.getPrompt() != null) {
                builder.part("prompt", request.getPrompt());
            }
            if (request.getResponseFormat() != null) {
                builder.part("response_format", request.getResponseFormat().getValue());
            }
            if (request.getTemperature() != null) {
                builder.part("temperature", request.getTemperature());
            }
            return builder;
        }

        private Class retrieveResponseClass(AudioResponseFormat fmt) {
            if (fmt == null) {
                return TranscriptionJsonResponse.class;
            }
            Class klazz = null;
            switch (fmt) {
                case JSON -> klazz = TranscriptionJsonResponse.class;
                case VERBOSE_JSON -> klazz = TranscriptionVerboseJsonResponse.class;
                default -> klazz = String.class;
            }
            return klazz;
        }

        private TranscriptionResponse textToTranscriptionResponse(Object value, AudioResponseFormat responseFormat) {
            if (value instanceof TranscriptionResponse) {
                return (TranscriptionResponse) value;
            }
            if (!(value instanceof String)) {
                throw new RuntimeException("openai response body not text/plain");
            }
            TranscriptionResponse parsed = null;
            String body = (String) value;
            switch (responseFormat) {
                case TEXT -> parsed = new TranscriptionJsonResponse(body);
                case SUB_RIP_TEXT -> parsed = TranscriptionSubRipTextResponse.parse(body);
                case VIDEO_TEXT_TRACKS -> parsed = TranscriptionVideoTextTracksResponse.parse(body);
            }
            return parsed;
        }

        @Override
        public CompletableFuture<Response<FineTuneFile>> listFineTuneFiles() {
            return webClient.get().uri("/v1/files")
                    .retrieve()
                    .bodyToMono(FILE_LIST_RESPONSE)
                    .toFuture();
        }

        @Override
        public CompletableFuture<FineTuneFile> uploadFineTuneFile(InputStream file, String fileName, String purpose) {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.asyncPart("file", DataBufferUtils.readInputStream(() -> file, DefaultDataBufferFactory.sharedInstance, BUFFER_SIZE), DataBuffer.class)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .filename(fileName);
            builder.part("purpose", purpose);
            return webClient.post().uri("/v1/files")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(FineTuneFile.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<FineTuneFile> retrieveFineTuneFile(String fileId) {
            return webClient.get().uri("/v1/files/{file_id}", fileId)
                    .retrieve()
                    .bodyToMono(FineTuneFile.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<DeleteResponse> deleteFineTuneFile(String fileId) {
            return webClient.delete().uri("/v1/files/{file_id}", fileId)
                    .retrieve()
                    .bodyToMono(DeleteResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<FineTuneResponse> fineTune(FineTuneRequest request) {
            return webClient.post().uri("/v1/fine-tunes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FineTuneResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<Response<FineTuneResponse>> listFineTunes() {
            return webClient.get().uri("/v1/fine-tunes")
                    .retrieve()
                    .bodyToMono(FINE_TUNE_LIST_RESPONSE)
                    .toFuture();
        }

        @Override
        public CompletableFuture<FineTuneResponse> retrieveFineTune(String fineTuneId) {
            return webClient.get().uri("/v1/fine-tunes/{fine_tune_id}", fineTuneId)
                    .retrieve()
                    .bodyToMono(FineTuneResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<FineTuneResponse> cancelFineTune(String fineTuneId) {
            return webClient.post().uri("/v1/fine-tunes/{fine_tune_id}/cancel", fineTuneId)
                    .retrieve()
                    .bodyToMono(FineTuneResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<Response<FineTuneEvent>> listFineTuneEvents(@NotNull String fineTuneId, Boolean stream) {
            StringBuilder queryParameters = new StringBuilder();
            if (stream != null && stream.booleanValue()) {
                queryParameters.append("?stream=").append(stream.booleanValue());
            }
            return webClient.post().uri("/v1/fine-tunes/{fine_tune_id}/events" + queryParameters.toString(), fineTuneId)
                    .retrieve()
                    .bodyToMono(FINE_TUNE_EVENT_LIST_RESPONSE)
                    .toFuture();
        }

        @Override
        public CompletableFuture<DeleteResponse> deleteFineTuneModel(String model) {
            return webClient.delete().uri("/v1/models/{model}", model)
                    .retrieve()
                    .bodyToMono(DeleteResponse.class)
                    .toFuture();
        }

        @Override
        public CompletableFuture<ModerationResponse> moderations(String input) {
            return webClient.post().uri("/v1/moderations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(String.format("{\"input\":\"%s\"}", input))
                    .retrieve()
                    .bodyToMono(ModerationResponse.class)
                    .toFuture();
        }
    }

}
