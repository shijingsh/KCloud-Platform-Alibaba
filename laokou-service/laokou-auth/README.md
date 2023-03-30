# spring-authorization-server 学习记录

 参考
https://blog.csdn.net/sjdl9396/article/details/122564492


## 目录

### 核心过滤链分析
      Spring 曾经有旧版支持 OAuth2 的方案：Spring Security OAuth 项目，该项目已经被逐步淘汰。官方推荐使用 spring-authorization-server

#### 授权服务器 SecurityFilterChain 过滤链 AuthorizationServerSecurityAutoConfiguration
      对符合的 URL 和 Method 的请求进行拦截  
      /oauth2/authorize
      /oauth2/consent
      /oauth2/token
      /oauth2/introspect
      /oauth2/revoke
      /oauth2/jwks  
####  OAuth2AuthorizationEndpointFilter
       这个过滤器用来处理授权码认证过程中获取 code 的请求。
       http://{{AuthorizationServer}}/oauth2/authorize?client_id=client&response_type=code&scope=testScope  
#### OAuth2ClientAuthenticationFilter
      客户端认证过滤器，主要是对请求的客户端进行认证。无论 grant type 是授权码还是客户端认证，请求中都会包含 client id 和 client secret。此过滤器就是确认客户端的 client id 和 client secret 是否正确。
#### OAuth2TokenEndpointFilter
      处理不同 Grant Type，并真正颁发 Token（AccessToken 和 RefreshToken）的过滤器。这个过滤器是颁发的核心
####  OAuth2TokenIntrospectionEndpointFilter
      授权服务器会颁发 Token，同时，也负责要验证颁发出的 Token 的有效性。此过滤器被调用用于确认 Token 的有效性，Token 有效则返回属于这个 Token 的一些认证授权信息。
      http://{{AuthorizationServer}}/oauth2/introspect?token={{AccessTokenUUID}}
####  OAuth2TokenRevocationEndpointFilter
      负责 Token 的注销
      http://{{AuthorizationServer}}/oauth2/revoke?token={{AccessTokenUUID}}

### 资源服务器 SecurityFilterChain 过滤链
       除授权服务器拦截的 url 外，其他任意请求都进资源服务器配置类的过滤器链。注意，任意请求的意思，也就是所有请求都要经过这里进行安全的鉴权和控制。这就是在 OAuth2 方案下进行鉴权的关键过滤器链了。

####  BearerTokenAuthenticationFilter
    在资源服务器的角度下，任何请求都需要验证该请求的有效性。请求中必须要附上 Token，那 Token 会经过此过滤器，调用认证管理器 AuthenticationManager 来对此 Token 进行校验（一般校验流程与上述授权服务器的 OAuth2TokenIntrospectionEndpointFilter 一致）。
    成功后，则把校验成功的信息存储在 SecurityContext 中，然后转到下一步的过滤器进行鉴权。


## 授权模式

### 客户端凭据许可：grant_type=client_credentials。  
```
    POST /oauth2/token?grant_type=client_credentials&scope=test1 test2 HTTP/1.1
    Host: localhost:8080
    Authorization: Basic Y2xpZW50OjEyMzQ1Ng==
```

    <1>对客户端信息进行认证

    /oauth2/token接口需要客户端认证通过才能访问，OAuth2ClientAuthenticationFilter（客户端认证过滤器）拦截请求，调用OAuth2ClientAuthenticationProvider（客户端认证管理器的提供者）
    对传入的client_id和client_secret进行判断。登录认证成功后，设置认证成功的结果（OAuth2ClientAuthenticationToken，内含客户端信息的RegisteredClient结果）到SecurityContext中。然后跳到下一步的过滤器中。

    <2>对客户端信息进行二次认证
    
    过滤器OAuth2TokenEndpointFilter继续拦截此请求，然后在颁发 Token 之前，请求OAuth2ClientCredentialsAuthenticationProvider（客户端凭据许可认证管理器的提供者）的authenticate()方法进行第二次认证。
    因为第一步的OAuth2ClientAuthenticationFilter已经校验过client_secret，这里主要对 client 的授权模式是否吻合，以及 scope 的授权范围进行校验就通过了。
    在OAuth2TokenEndpointFilter中，调用 OAuth2XxxxAuthenticationProvider 的 authenticate() 方法进行认证是固定流程。实际会根据不同的grant_type去选择调用不同的 
    OAuth2XxxxAuthenticationProvider 进行认证。因此，对客户端信息进行了二次认证，第一次和第二次的认证的职责是不通的。
