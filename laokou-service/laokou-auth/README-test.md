# spring-authorization-server 接口路径

###
增加
.redirectUri("https://www.baidu.com")
.redirectUri("https://cn.bing.com")

```
	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("messaging-client")
				.clientSecret("{noop}secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.redirectUri("http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc")
				.redirectUri("http://127.0.0.1:8080/authorized")
				.redirectUri("https://www.baidu.com")
				.redirectUri("https://cn.bing.com")
				.postLogoutRedirectUri("http://127.0.0.1:8080/index")
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.scope("message.read")
				.scope("message.write")
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
				.build();
		return new InMemoryRegisteredClientRepository(registeredClient);
	}
```

### 授权码 获取令牌
http://localhost:9000/oauth2/authorize?client_id=messaging-client&response_type=code&scope=message.read&redirect_uri=https://www.baidu.com

http://localhost:9000/oauth2/authorize?response_type=code&client_id=messaging-client&scope=message.read&redirect_uri=https://cn.bing.com

http://localhost:9000/oauth2/authorize?response_type=code&client_id=messaging-client&scope=openid%20profile&state=ntSwy-r7oA-pVy-mk8RmDS1cNOid3MC0wfcojbpc6bA%3D&redirect_uri=http://127.0.0.1:8080/login/oauth2/code/messaging-client-oidc&nonce=CO99hyz--jBU23V08r2SWyF_ryHCVb8026TfTi9xzkQ&continue

###  通过 code 获取令牌
   使用postman发送请求 POST /oauth2/token，需要注意的是客户端的信息以 Basic 方式认证
code：
   98_9Z3b5teVHSmonUlrQ0Uhxxh5wJBZiaMbvxLlAgtuosTXbhjF1FQnDRNz3QjO_NPESTe8u1Wy_8IQS1_16e1zGD6WDbj8iYKmOI2FctfxMgkRPZeCrAu8XBCrDTU4t
   8PxbInDl-QST8IMG3fIET48KOglCz57oewHmkmMs-KvxEHygUjfQs2RoCxE2mxqhX59tzG1rZzuZ95Jf1RcyiRi_8QqS9STUdOAcUx130K_Nj2zbb52KvFTCtRXlIeX4

authorization 选择Basic Auth
userName和password 中输入client_id 和 secret (注意：不是用户名、密码)

Body中选择 form-data
grant_type : authorization_code
code : ...
redirect_uri :https://cn.bing.com

{
"access_token":"eyJraWQiOiI2YzJlNjUzMS02MDRiLTRmOGUtOGJkNi0zNmVhOTlhMjc5YjAiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImF1ZCI6Im1lc3NhZ2luZy1jbGllbnQiLCJuYmYiOjE2ODAxNjU2NjgsInNjb3BlIjpbIm1lc3NhZ2UucmVhZCJdLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjkwMDAiLCJleHAiOjE2ODAxNjU5NjgsImlhdCI6MTY4MDE2NTY2OH0.rRsZ5KN8l8BMBw0wLlVbIY0pPxFonLX5WNps8cBHY7wBvq_0Okjt7gbBoxF8gCH2B9ZY6kfHFrrUgIEdisLhIk1C4jNzuQTAZ9-X-BQUO0PtK9gJpg0sijEehgi-jRLfoqxqaKqIqf51dcAyBxlf4SoUstwOiZW3Gm5VgbapGpUzRJo1nYnIn1k9B-srTZXRlMqmmcFjfedMPJALFlMjHIRuvziVg2immPWLZyhNBrRGcOFxD--hNYoeydz289-iGGeZ9nTmtoNUkpag7VsFmzMl1RlPCNOeL7WrihFGNRffZ8Lq8PAd_-2K8_j7zg6_79hNzDp7wGwgtjfayIyOxA",
"refresh_token":"1FpBg3QbAqCCnLQGmhcBY062FgroNbw-4GFtq2GbxOqRXPwqqTPJ86CIItD-1hfp6etfySLx_yMdNWU6s5t6SqtLLHr6inE_pF1E8UWrbOTqWMNBj0CPOlHLIVa9cBWD",
"scope":"message.read",
"token_type":"Bearer",
"expires_in":300
}


### 刷新令牌
    同样是 POST /oauth2/token 接口，仅参数不同，客户端的信息同样以 Basic 方式认证
    使用上步骤获取的 refresh_token 测试。

grant_type : refresh_token
refresh_token : ...


### 客户端凭证
    同样是 POST /oauth2/token 接口，仅参数不同，客户端的信息同样以 Basic 方式认证

grant_type : client_credentials
