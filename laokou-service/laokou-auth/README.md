# spring-authorization-server 学习记录

 参考
https://blog.csdn.net/sjdl9396/article/details/122564492


## 目录

1. 选型
   Spring 曾经有旧版支持 OAuth2 的方案：Spring Security OAuth 项目，该项目已经被逐步淘汰。官方推荐使用 spring-authorization-server
2. 核心过滤链分析
   2.1. 授权服务器 SecurityFilterChain 过滤链 AuthorizationServerSecurityAutoConfiguration
   2.1.1. OAuth2AuthorizationEndpointFilter
   2.1.2. OAuth2ClientAuthenticationFilter
   2.1.3. OAuth2TokenEndpointFilter
   2.1.4. OAuth2TokenIntrospectionEndpointFilter
   2.1.5. OAuth2TokenRevocationEndpointFilter

### 资源服务器 SecurityFilterChain 过滤链