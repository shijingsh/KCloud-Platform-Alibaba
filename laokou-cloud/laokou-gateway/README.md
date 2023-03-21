
# gateway 知识点记录

### gateway 实现
    gateway是基于webflux的，和springboot-mvc 冲突， 因此不要引入spring-boot-starter-web依赖！
#### 全局过滤器
    全局过滤器，用来添加自己的逻辑

```java
@Order(-1)  // order指定的越小，同等级Filter就先执行
@Component
public class AuthFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 如果满足条件就放行（执行下一个过滤器）
        if(request.getQueryParams().get("auth") != null){
            return chain.filter(exchange);
        }

        // 如果不满足条件就重定向到 "https://www.baidu.com/"
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SEE_OTHER);
        response.getHeaders().set("Location", "https://www.baidu.com/");
        return exchange.getResponse().setComplete();
    }
}

```

#### 网关路由的流程图
    用户请求-->GlobalFilter
        -->路由断言 predicates
        -->userFilter  
        -->LoadBalancerClient  -->实际的服务主机和端口，并进行ribbon负载均衡
        -->目标服务

### caffeine 缓存

    Caffeine是一款基于java8开发的本地缓存框架，拥有着较高的性能、命中率以及优秀的缓存驱逐策略。
    它与ConcurrentHashMap非常相似的，但后者并不能自动的移除那些访问不频繁的数据。

#### 引入依赖

```xml
<dependencys>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencys>
```

#### 开启缓存
启动类添加 @EnableCaching注解，开启缓存使用
```java
@SpringBootApplication
@EnableCaching
public class GatewayApplication {
    
}
```

#### 配置缓存
```java
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(Cache.class)
    public Cache<String, RouteDefinition> caffeineCache() {
        return Caffeine.newBuilder()
                // 初始化10个容量
                .initialCapacity(10)
                .build();
    }
}
```

### feign的配置
     Spring OpenFeign是一个轻量级的http请求调用框架，通过面向接口的方式，让微服务之间的接口调用变得简单

#### 依赖
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

#### 给需要使用远程调用功能的服务的启动类添加 @EnableFeignClients 注解

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
@Import(BeanValidatorPluginsConfiguration.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
    }
}
```

#### Feign客户端（接口）
```java
@FeignClient("userservice") //服务名
public interface UserFeign {

    // 请求方式、请求路径、方法参数、返回值要和 被请求的服务 的接口相同
    @GetMapping("/user/{id}")   
    User findByID(@PathVariable("id")Long id);
}

@Component
class Test{

    @Autowired
    private UserFeign userFeign;

    public void test(){
        User user = userFeign.findByID(1L);
    }
}
```
#### Feign设置
    Fegin内置的ribbon默认设置了请求超时时长，默认是1000

```yml
ribbon: 
  ReadTimeout: 2000 # 读取超时时长
  ConnectTimeout: 1000 # 建立链接的超时时长
```
    因为ribbon内部有重试机制，一旦超时，会自动重新发起请求
```yml
ribbon:
  ConnectTimeout: 1000 # 连接超时时长
  ReadTimeout: 2000 # 数据通信超时时长
  MaxAutoRetries: 0 # 当前服务器的重试次数
  MaxAutoRetriesNextServer: 0 # 重试多少次服务
  OkToRetryOnAllOperations: false # 是否对所有的请求方式都重试
```
    Feign开启Hystrix 熔断
```yml
feign:
  hystrix:
    enabled: true # 开启Feign的熔断功能
```
    开启请求与响应的压缩功能
```yml
feign:
  hystrix:
    enabled: true # 开启Feign的熔断功能
  compression:
    request:
      enabled: true # 开启请求压缩
    response:
      enabled: true
      #请求的数据类型，以及触发压缩的大小下限进行设置
      mime-types: text/html,application/xml,application/json # 设置压缩的数据类型
      min-request-size: 2048 # 设置触发压缩的大小下限
```
