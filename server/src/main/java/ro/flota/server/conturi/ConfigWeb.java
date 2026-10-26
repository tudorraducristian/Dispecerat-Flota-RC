package ro.flota.server.conturi;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Interceptorul stă pe tot /api/**, mai puțin pe login, singura ușă deschisă fără token. */
@Configuration
public class ConfigWeb implements WebMvcConfigurer {

    private final InterceptorAutentificare interceptor;

    public ConfigWeb(InterceptorAutentificare interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login");
    }
}
