package in.civictrack;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Set;

final class Workflow {
 private Workflow() {}
 static void require(boolean condition,String message) { if(!condition) throw new ResponseStatusException(HttpStatus.FORBIDDEN,message); }
 static boolean manager(Profile p,Complaint c) { return p.role==Role.ADMIN || p.role==Role.SUPERVISOR && p.department!=null && p.department.equals(c.department) && p.area!=null && p.area.equals(c.area); }
 static boolean canRead(Profile p,Complaint c) { return p.id.equals(c.reporterId) || manager(p,c) || p.role==Role.OFFICER && p.id.equals(c.officerId) && p.department!=null && p.department.equals(c.department) && p.area!=null && p.area.equals(c.area); }
 static void transition(Profile p,Complaint c,Status target,boolean evidence) {
  boolean owner=p.id.equals(c.reporterId), manager=manager(p,c), officer=p.role==Role.OFFICER && p.id.equals(c.officerId) && canRead(p,c);
  boolean allowed=switch(target) {
   case ACKNOWLEDGED -> manager && c.status==Status.SUBMITTED;
   case NEEDS_INFORMATION, REJECTED -> manager && c.status==Status.SUBMITTED;
   case SUBMITTED -> owner && c.status==Status.NEEDS_INFORMATION;
   case IN_PROGRESS -> officer && c.status==Status.ASSIGNED;
   case RESOLUTION_PROPOSED -> officer && c.status==Status.IN_PROGRESS && evidence;
   case CLOSED -> owner && c.status==Status.RESOLUTION_PROPOSED;
   case REOPENED -> owner && Set.of(Status.RESOLUTION_PROPOSED,Status.CLOSED).contains(c.status) && c.proposedAt!=null && c.proposedAt.plusSeconds(7*86400).isAfter(Instant.now());
   default -> false;
  };
  require(allowed,"This transition is not permitted. Resolution requires evidence; reopening is allowed within seven days of proposal.");
 }
}
