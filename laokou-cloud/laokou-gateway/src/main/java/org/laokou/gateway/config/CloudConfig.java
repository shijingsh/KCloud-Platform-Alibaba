/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.gateway.config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import java.util.stream.Collectors;
/**
 * 消息转换器配置
 * 利用SpringMVC框架，可以使得我们在开发时，只要在代码中使用@RequestBody和@ResponseBody两个注解，就可以分别完成从请求报文到对象和从对象到响应报文的转换。而在源码内部，其实这种灵活的消息转换机制就是利用HttpMessageConverter来实现的。
 *
 * HttpMessageConverter的调用是RequestResponseBodyMethodProcessor类的解析请求参数的方法resolveArgument()和处理返回值的方法handleReturnValue()中进行调用的。这是关于@RequestBody和@ResponseBody两个注解的原理。
 * 消息转换器是在项目启动的时候通过WebMvcConfigurationSupport进行加载
 * @author laokou
 */
@Configuration
public class CloudConfig {

    /**
     * 在gateway下整合feign，SpringCloud中通过Feign接口实现服务的远程调用，对于数据的序列化和反序列化依旧借助了HttpMessageConverter（消息转换器），
     * Spring Cloud Gateway是基于WebFlux的，是ReactiveWeb。所以HttpMessageConverters不会自动注入。
     * @param converters
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(HttpMessageConverters.class)
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
    }
}
