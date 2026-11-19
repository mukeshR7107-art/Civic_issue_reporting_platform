package in.civictrack;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration
class SecurityConfig {
 @Bean JwtDecoder decoder(@Value("${app.supabase-url}") String url) {
  NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(url+"/auth/v1/.well-known/jwks.json")
    .jwsAlgorithms(a->{a.add(SignatureAlgorithm.RS256); a.add(SignatureAlgorithm.ES256);}).build();
  OAuth2TokenValidator<Jwt> audience=jwt->jwt.getAudience().contains("authenticated")
    ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Invalid audience",null));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(url+"/auth/v1"),audience));
  return decoder;
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(a->a.requestMatchers("/api/health","/api/config","/","/index.html","/assets/**","/favicon.svg").permitAll().anyRequest().authenticated())
    .oauth2ResourceServer(o->o.jwt(j->{})).build();
 }
 @Bean CorsConfigurationSource cors(@Value("${app.allowed-origin}") String origin) {
  var c=new CorsConfiguration(); c.setAllowedOrigins(List.of(origin));
  c.setAllowedMethods(List.of("GET","POST","PATCH","PUT","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type"));
  var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",c); return source;
 }
}
