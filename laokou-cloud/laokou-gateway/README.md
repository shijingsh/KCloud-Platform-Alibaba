
# gateway 知识点记录

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