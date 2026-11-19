package in.civictrack;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api") @Transactional
class StorageApi {
 final ComplaintService service; final RestClient storage; final String key;
 StorageApi(ComplaintService s,@Value("${app.supabase-url}") String url,@Value("${app.supabase-service-key}") String key){service=s;this.key=key;storage=RestClient.builder().baseUrl(url+"/storage/v1").defaultHeader("Authorization","Bearer "+key).defaultHeader("apikey",key).build();}
 @PostMapping("/complaints/{id}/attachments") Attachment upload(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam MultipartFile file,@RequestParam String kind) throws IOException {
  Profile p=service.actor(jwt);Complaint c=service.get(p,id);
  Workflow.require(kind.equals("REPORT")&&p.id.equals(c.reporterId)&&Set.of(Status.SUBMITTED,Status.NEEDS_INFORMATION).contains(c.status)||kind.equals("RESOLUTION")&&p.role==Role.OFFICER&&p.id.equals(c.officerId)&&c.status==Status.IN_PROGRESS,"You cannot attach this kind of evidence now");
  if(key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Image storage has not been configured");
  if(!key.startsWith("eyJ"))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Storage needs a legacy service_role JWT for direct private-object access. Keep the newer sb_secret key for API gateway calls.");
  if(file.isEmpty()||file.getSize()>3*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a JPEG or PNG under 3 MB");
  byte[] bytes=file.getBytes();String type;
  if(bytes.length>8&&(bytes[0]&255)==137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)type="image/png";
  else if(bytes.length>3&&(bytes[0]&255)==255&&(bytes[1]&255)==216&&(bytes[2]&255)==255)type="image/jpeg";
  else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only JPEG and PNG images are accepted");
  // Inspect dimensions before decoding, then re-encode to remove EXIF/location metadata.
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
   var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image");
   var reader=readers.next();try{reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>20_000_000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image exceeds 20 megapixels");
    var out=new ByteArrayOutputStream();ImageIO.write(reader.read(0),type.equals("image/png")?"png":"jpeg",out);bytes=out.toByteArray();
   }finally{reader.dispose();}
  }
  if(bytes.length>3*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Processed image exceeds 3 MB");
  Attachment a=new Attachment();a.complaintId=id;a.uploadedBy=p.id;a.kind=kind;a.contentType=type;a.objectPath=id+"/"+a.id+(type.equals("image/png")?".png":".jpg");
  storage.post().uri("/object/civic-evidence/"+a.objectPath).contentType(MediaType.parseMediaType(type)).body(bytes).retrieve().toBodilessEntity();
  service.attachments().save(a);service.event(p,c,"EVIDENCE",kind.equals("REPORT")?"Report photograph added":"Resolution photograph added",false);return a;
 }
 @GetMapping("/attachments/{id}/url") Map<?,?> signed(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){Attachment a=service.attachments().findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));service.get(service.actor(jwt),a.complaintId);return storage.post().uri("/object/sign/civic-evidence/"+a.objectPath).body(Map.of("expiresIn",120)).retrieve().body(Map.class);}
}
