package in.civictrack;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
@RestController
class PublicConfig {
 private final String url,key;
 PublicConfig(@Value("${app.supabase-url}") String url,@Value("${app.supabase-publishable-key:}") String key){if(!key.isEmpty()&&!key.startsWith("sb_publishable_"))throw new IllegalArgumentException("SUPABASE_PUBLISHABLE_KEY must be a publishable key, never a secret/service key");this.url=url;this.key=key;}
 @GetMapping("/api/config") Map<String,String> config(){return Map.of("supabaseUrl",url,"supabasePublishableKey",key);}
}
