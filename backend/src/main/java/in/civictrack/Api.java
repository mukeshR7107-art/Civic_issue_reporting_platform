package in.civictrack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api") @Transactional
class Api {
 static final List<String> CATEGORIES=List.of("Streetlight","Garbage","Pothole","Water leakage","Public facility");
 static final List<String> AREAS=List.of("Ward A","Ward B","Ward C");
 final ComplaintService service;
 Api(ComplaintService service) {this.service=service;}
 record NewReport(@NotBlank @Size(max=160) String title,@NotBlank @Size(min=15,max=4000) String description,@NotBlank String category,@NotBlank String area,@NotBlank @Size(max=255) String landmark,@DecimalMin("-90") @DecimalMax("90") double latitude,@DecimalMin("-180") @DecimalMax("180") double longitude,@Pattern(regexp="LOW|NORMAL|HIGH") String urgency,@NotNull UUID requestKey) {}
 record Change(@NotNull Status status,@NotBlank @Size(max=2000) String reason,@Min(0) long version) {}
 record Assign(@NotNull UUID officerId,@NotBlank @Size(max=2000) String reason,@Min(0) long version) {}
 record Comment(@NotBlank @Size(max=4000) String message,boolean internal) {}
 record PriorityChange(@NotNull Priority priority,@NotBlank @Size(max=2000) String reason,@Min(0) long version) {}
 record StaffChange(@NotNull Role role,@NotBlank @Size(max=100) String name,@Size(max=100) String department,@Size(max=100) String area) {}
 record RouteChange(@NotBlank String category,@NotBlank String area,@NotBlank @Size(max=100) String department) {}
 @GetMapping("/health") Map<String,String> health(){return Map.of("status","ok","service","civictrack-api");}
 @GetMapping("/me") Profile me(@AuthenticationPrincipal Jwt jwt){return service.actor(jwt);}
 @GetMapping("/catalog") Map<String,Object> catalog(){return Map.of("categories",CATEGORIES,"areas",AREAS);}
 @GetMapping("/complaints") Page<Complaint> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String q,@RequestParam(required=false) Status status,@RequestParam(required=false) String area,@RequestParam(required=false) String category,@RequestParam(required=false) Priority priority,@RequestParam(defaultValue="0") int page) {return service.list(service.actor(jwt),q,status,area,category,priority,page);}
 @PostMapping("/complaints") Complaint create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody NewReport report){return service.create(service.actor(jwt),report);}
 @GetMapping("/complaints/{id}") Map<String,Object> detail(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
  Profile p=service.actor(jwt);Complaint c=service.get(p,id);
  return Map.of("complaint",c,"events",service.events().findByComplaintIdOrderByCreatedAtAsc(id).stream().filter(e->!e.internal||Workflow.manager(p,c)||p.role==Role.OFFICER&&p.id.equals(c.officerId)).toList(),"attachments",service.attachments().findByComplaintId(id));
 }
 @PostMapping("/complaints/{id}/status") Complaint status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Change request){return service.transition(service.actor(jwt),id,request);}
 @PostMapping("/complaints/{id}/assignment") Complaint assign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Assign request){return service.assign(service.actor(jwt),id,request);}
 @PostMapping("/complaints/{id}/comments") Map<String,String> comment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Comment request){Profile p=service.actor(jwt);Complaint c=service.get(p,id);Workflow.require(!request.internal()||Workflow.manager(p,c)||p.role==Role.OFFICER&&p.id.equals(c.officerId),"Only staff may add internal notes");service.event(p,c,"COMMENT",request.message(),request.internal());return Map.of("status","saved");}
 @PostMapping("/complaints/{id}/priority") Complaint priority(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody PriorityChange r){Profile p=service.actor(jwt);Complaint c=service.get(p,id);service.version(c,r.version());Workflow.require(Workflow.manager(p,c),"Only responsible staff may set operational priority");c.priority=r.priority();service.event(p,c,"PRIORITY",r.priority()+": "+r.reason(),false);return c;}
 @GetMapping("/staff") List<Profile> staff(@AuthenticationPrincipal Jwt jwt){Profile p=service.actor(jwt);Workflow.require(p.role==Role.ADMIN||p.role==Role.SUPERVISOR,"Staff access required");return service.profiles().findByRole(Role.OFFICER).stream().filter(o->p.role==Role.ADMIN||Objects.equals(p.department,o.department)&&Objects.equals(p.area,o.area)).toList();}
 @PutMapping("/admin/staff/{id}") Profile staff(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody StaffChange r){Profile p=service.actor(jwt);Workflow.require(p.role==Role.ADMIN,"Administrator access required");Workflow.require(!p.id.equals(id),"Cannot change your own permissions");Profile other=service.profiles().findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User must sign in first"));if(r.role()!=Role.CITIZEN&&r.role()!=Role.ADMIN&&(r.department()==null||r.department().isBlank()||!AREAS.contains(r.area())))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Staff require a department and valid area");other.role=r.role();other.name=r.name();other.department=r.department();other.area=r.area();return service.profiles().save(other);}
 @GetMapping("/admin/routes") List<RoutingRule> routes(@AuthenticationPrincipal Jwt jwt){Workflow.require(service.actor(jwt).role==Role.ADMIN,"Administrator access required");return service.routes().findAll();}
 @PostMapping("/admin/routes") RoutingRule route(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RouteChange r){Workflow.require(service.actor(jwt).role==Role.ADMIN,"Administrator access required");if(!CATEGORIES.contains(r.category())||!AREAS.contains(r.area()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown category or area");RoutingRule route=service.routes().findByCategoryAndArea(r.category(),r.area()).orElseGet(RoutingRule::new);route.category=r.category();route.area=r.area();route.department=r.department();return service.routes().save(route);}
 @GetMapping("/notifications") List<Notification> notifications(@AuthenticationPrincipal Jwt jwt){return service.notifications().findTop50ByUserIdOrderByCreatedAtDesc(service.actor(jwt).id);}
 @PostMapping("/notifications/{id}/read") void read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){Profile p=service.actor(jwt);Notification n=service.notifications().findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));Workflow.require(n.userId.equals(p.id),"Not your notification");n.seen=true;}
}
