package io.github.afgprojects.framework.core.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.*;
import io.github.afgprojects.framework.core.invocation.interceptor.AuditInvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.interceptor.ValidationInvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.processor.IdentityProcessor;
import io.github.afgprojects.framework.core.invocation.processor.PagedResultProcessor;
import io.github.afgprojects.framework.core.invocation.processor.ResultProcessor;
import io.github.afgprojects.framework.core.invocation.processor.SensitiveMaskProcessor;
import io.github.afgprojects.framework.core.invocation.resolver.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(BeanInvocationProperties.class)
@ConditionalOnProperty(prefix = "afg.invocation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BeanInvocationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceMetadataRegistry serviceMetadataRegistry() {
        return new DefaultServiceMetadataRegistry();
    }

    @Bean
    public AptServiceMetadataLoader aptServiceMetadataLoader(ServiceMetadataRegistry registry) {
        AptServiceMetadataLoader loader = new AptServiceMetadataLoader(registry);
        loader.load();
        return loader;
    }

    @Bean
    @ConditionalOnMissingBean
    public BeanInvocationEngine beanInvocationEngine(
            ServiceMetadataRegistry registry,
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            @Autowired(required = false) List<InvocationInterceptor> interceptors,
            @Autowired(required = false) List<ArgumentResolver> resolvers,
            @Autowired(required = false) List<ResultProcessor> resultProcessors) {

        List<InvocationInterceptor> allInterceptors = new ArrayList<>();
        if (interceptors != null) allInterceptors.addAll(interceptors);
        allInterceptors.add(new AuditInvocationInterceptor());

        List<ArgumentResolver> allResolvers = new ArrayList<>();
        if (resolvers != null) allResolvers.addAll(resolvers);
        allResolvers.add(new IdentityResolver());
        allResolvers.add(new JacksonConvertResolver());
        allResolvers.add(new StringConverterResolver());
        allResolvers.add(new CollectionResolver());
        allResolvers.add(new NullDefaultResolver());

        List<ResultProcessor> allProcessors = new ArrayList<>();
        if (resultProcessors != null) allProcessors.addAll(resultProcessors);
        allProcessors.add(new SensitiveMaskProcessor());
        allProcessors.add(new PagedResultProcessor());
        allProcessors.add(new IdentityProcessor());

        return new DefaultBeanInvocationEngine(
                registry,
                serviceName -> applicationContext.getBean(serviceName),
                allInterceptors,
                allResolvers,
                allProcessors,
                objectMapper
        );
    }
}