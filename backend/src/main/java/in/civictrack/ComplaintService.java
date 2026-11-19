package in.civictrack;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Service @Transactional
class ComplaintService {
 final Profiles profiles; final Complaints complaints; final Events events; final Attachments attachments; final Notifications notifications; final Routes routes;
 ComplaintService(Profiles p,Complaints c,Events e,Attachments a,Notifications n,Routes r) {profiles=p;complaints=c;events=e;attachments=a;notifications=n;routes=r;}
 Profiles profiles(){return profiles;}
 Events events(){return events;}
 Attachments attachments(){return attachments;}
 Notifications notifications(){return notifications;}
 Routes routes(){return routes;}
 Profile actor(Jwt jwt) {
  UUID id=UUID.fromString(jwt.getSubject());
  return profiles.findById(id).orElseGet(()->profiles.save(new Profile(id,"Resident")));
 }
 Complaint get(Profile actor,UUID id) {
  Complaint c=complaints.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Report not found"));
  Workflow.require(Workflow.canRead(actor,c),"You cannot access this report"); return c;
 }
 Page<Complaint> list(Profile p,String q,Status status,String area,String category,Priority priority,int page) {
  Specification<Complaint> spec=(root,query,cb)->{
   List<Predicate> filters=new ArrayList<>();
   if(p.role!=Role.ADMIN) {
    Predicate own=cb.equal(root.get("reporterId"),p.id);
    if(p.role==Role.SUPERVISOR) own=cb.or(own,cb.and(cb.equal(root.get("department"),p.department),cb.equal(root.get("area"),p.area)));
    if(p.role==Role.OFFICER) own=cb.or(own,cb.and(cb.equal(root.get("officerId"),p.id),cb.equal(root.get("department"),p.department),cb.equal(root.get("area"),p.area)));
    filters.add(own);
   }
   if(q!=null&&!q.isBlank()) {String term="%"+q.toLowerCase().replace("%","").replace("_","")+"%";filters.add(cb.or(cb.like(cb.lower(root.get("title")),term),cb.like(cb.lower(root.get("trackingNumber")),term)));}
   if(status!=null) filters.add(cb.equal(root.get("status"),status));
   if(area!=null&&!area.isBlank()) filters.add(cb.equal(root.get("area"),area));
   if(category!=null&&!category.isBlank()) filters.add(cb.equal(root.get("category"),category));
   if(priority!=null) filters.add(cb.equal(root.get("priority"),priority));
   return cb.and(filters.toArray(Predicate[]::new));
  };
  return complaints.findAll(spec,PageRequest.of(Math.max(0,page),30,Sort.by("createdAt").descending()));
 }
 Complaint create(Profile p,Api.NewReport r) {
  Optional<Complaint> prior=complaints.findByReporterIdAndRequestKey(p.id,r.requestKey()); if(prior.isPresent()) return prior.get();
  if(!Api.CATEGORIES.contains(r.category())||!Api.AREAS.contains(r.area())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown category or area");
  Complaint c=new Complaint(); c.trackingNumber="CT-"+c.id.toString().substring(0,8).toUpperCase(); c.reporterId=p.id;c.requestKey=r.requestKey();
  c.title=r.title().trim();c.description=r.description().trim();c.category=r.category();c.area=r.area();c.landmark=r.landmark();c.latitude=r.latitude();c.longitude=r.longitude();c.urgency=r.urgency();
  c.department=routes.findByCategoryAndArea(c.category,c.area).map(x->x.department).orElse(null);
  complaints.saveAndFlush(c); event(p,c,"SUBMITTED","Report submitted. "+(c.department==null?"Awaiting routing review.":"Routed to "+c.department+"."),false);return c;
 }
 void version(Complaint c,long version) {if(c.version!=version) throw new ResponseStatusException(HttpStatus.CONFLICT,"This report has changed. Refresh and try again.");}
 void event(Profile p,Complaint c,String action,String message,boolean internal) {
  StatusEvent e=new StatusEvent();e.complaintId=c.id;e.actorId=p.id;e.actorName=p.name;e.action=action;e.message=message;e.internal=internal;events.save(e);
  c.updatedAt=Instant.now();
  if(!internal&&!p.id.equals(c.reporterId)) notify(c.reporterId,c,action.replace('_',' ')+": "+message);
 }
 void notify(UUID user,Complaint c,String message) {Notification n=new Notification();n.userId=user;n.complaintId=c.id;n.message=message.substring(0,Math.min(message.length(),4000));notifications.save(n);}
 Complaint transition(Profile p,UUID id,Api.Change r) {
  Complaint c=get(p,id);version(c,r.version());Workflow.transition(p,c,r.status(),c.workStartedAt!=null&&attachments.existsByComplaintIdAndKindAndCreatedAtAfter(id,"RESOLUTION",c.workStartedAt));
  c.status=r.status();if(c.status==Status.IN_PROGRESS)c.workStartedAt=Instant.now();if(c.status==Status.RESOLUTION_PROPOSED)c.proposedAt=Instant.now();if(c.status==Status.CLOSED)c.closedAt=Instant.now();if(c.status==Status.REOPENED)c.closedAt=null;
  event(p,c,c.status.name(),r.reason(),false);return complaints.save(c);
 }
 Complaint assign(Profile p,UUID id,Api.Assign r) {
  Complaint c=get(p,id);version(c,r.version());Workflow.require(Workflow.manager(p,c),"Only the responsible supervisor or administrator may assign work");
  Workflow.require(Set.of(Status.ACKNOWLEDGED,Status.ASSIGNED,Status.REOPENED,Status.IN_PROGRESS).contains(c.status),"Acknowledge the report before assignment");
  Profile officer=profiles.findById(r.officerId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Officer not found"));
  Workflow.require(officer.role==Role.OFFICER && Objects.equals(officer.department,c.department)&&Objects.equals(officer.area,c.area),"Officer must serve this department and area");
  String previous=c.officerId==null?"Unassigned":c.officerId.toString();c.officerId=officer.id;c.status=Status.ASSIGNED;
  event(p,c,"ASSIGNED",previous+" → "+officer.name+". "+r.reason(),false);notify(officer.id,c,"Assigned: "+c.title);return complaints.save(c);
 }
}
