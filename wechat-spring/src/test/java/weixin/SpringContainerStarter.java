package weixin;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import weixin.mp.infrastructure.config.SimpleConfiguration;
import weixin.mp.infrastructure.config.SpringConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.WHEN_AVAILABLE,
        classes = {Application.class, SpringConfiguration.class, SimpleConfiguration.class}, // spring-boot-test bug?
        properties = "spring.main.web-application-type=reactive"
)
@ActiveProfiles("junit")
public abstract class SpringContainerStarter {

    // Caused by: java.lang.IllegalAccessException: class org.mockito.internal.util.reflection.ReflectionMemberAccessor cannot access class weixin.SpringContainerStarter (in module wechat.spring) because module wechat.spring does not export weixin to unnamed module @7920ba90
//    @Autowired
//    protected WebTestClient webClient;
//
//    @Mock
//    protected ClientHttpConnector clientHttpConnector;
//
//    @Mock
//    protected ClientHttpResponse clientHttpResponse;
}
