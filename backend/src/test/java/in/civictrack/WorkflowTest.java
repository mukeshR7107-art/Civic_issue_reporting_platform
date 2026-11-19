package in.civictrack;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class WorkflowTest {
 Profile citizen=new Profile(UUID.randomUUID(),"Citizen");
 Profile supervisor=staff(Role.SUPERVISOR,"Electrical maintenance","Ward A");
 Profile officer=staff(Role.OFFICER,"Electrical maintenance","Ward A");
 static Profile staff(Role role,String department,String area){Profile p=new Profile(UUID.randomUUID(),"Staff");p.role=role;p.department=department;p.area=area;return p;}
 Complaint report(){Complaint c=new Complaint();c.reporterId=citizen.id;c.department="Electrical maintenance";c.area="Ward A";c.officerId=officer.id;return c;}
 @Test void unrelatedCitizenCannotRead(){assertFalse(Workflow.canRead(new Profile(UUID.randomUUID(),"Other"),report()));}
 @Test void supervisorIsScopedByDepartmentAndArea(){assertTrue(Workflow.canRead(supervisor,report()));assertFalse(Workflow.canRead(staff(Role.SUPERVISOR,"Sanitation","Ward A"),report()));assertFalse(Workflow.canRead(staff(Role.SUPERVISOR,"Electrical maintenance","Ward B"),report()));}
 @Test void onlyAssignedOfficerCanRead(){assertTrue(Workflow.canRead(officer,report()));assertFalse(Workflow.canRead(staff(Role.OFFICER,"Electrical maintenance","Ward A"),report()));}
 @Test void changedMembershipRevokesAccess(){officer.department="Sanitation";assertFalse(Workflow.canRead(officer,report()));}
 @Test void citizenCannotAcknowledgeOrSelfClose(){assertThrows(ResponseStatusException.class,()->Workflow.transition(citizen,report(),Status.ACKNOWLEDGED,false));assertThrows(ResponseStatusException.class,()->Workflow.transition(citizen,report(),Status.CLOSED,false));}
 @Test void supervisorMayAcknowledge(){assertDoesNotThrow(()->Workflow.transition(supervisor,report(),Status.ACKNOWLEDGED,false));}
 @Test void officerCannotSkipWork(){Complaint c=report();c.status=Status.ASSIGNED;assertThrows(ResponseStatusException.class,()->Workflow.transition(officer,c,Status.RESOLUTION_PROPOSED,true));}
 @Test void resolutionRequiresPhoto(){Complaint c=report();c.status=Status.IN_PROGRESS;assertThrows(ResponseStatusException.class,()->Workflow.transition(officer,c,Status.RESOLUTION_PROPOSED,false));assertDoesNotThrow(()->Workflow.transition(officer,c,Status.RESOLUTION_PROPOSED,true));}
 @Test void officerCannotConfirmOwnResolution(){Complaint c=report();c.status=Status.RESOLUTION_PROPOSED;assertThrows(ResponseStatusException.class,()->Workflow.transition(officer,c,Status.CLOSED,true));assertDoesNotThrow(()->Workflow.transition(citizen,c,Status.CLOSED,true));}
 @Test void citizenCanReopenWithinWindow(){Complaint c=report();c.status=Status.CLOSED;c.proposedAt=Instant.now();assertDoesNotThrow(()->Workflow.transition(citizen,c,Status.REOPENED,true));c.proposedAt=Instant.now().minusSeconds(8*86400);assertThrows(ResponseStatusException.class,()->Workflow.transition(citizen,c,Status.REOPENED,true));}
}
