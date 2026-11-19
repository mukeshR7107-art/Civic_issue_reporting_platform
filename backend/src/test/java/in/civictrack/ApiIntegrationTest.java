package in.civictrack;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired Profiles profiles;
 @Autowired Complaints complaints;
 @Autowired Events events;
 @Autowired Attachments attachments;
 UUID citizen=UUID.randomUUID(),supervisor=UUID.randomUUID(),officer=UUID.randomUUID();
 @BeforeEach void setup(){profiles.save(new Profile(citizen,"Citizen"));Profile s=new Profile(supervisor,"Supervisor");s.role=Role.SUPERVISOR;s.department="Electrical maintenance";s.area="Ward A";profiles.save(s);Profile o=new Profile(officer,"Officer");o.role=Role.OFFICER;o.department=s.department;o.area=s.area;profiles.save(o);}
 Complaint report(){Complaint c=new Complaint();c.reporterId=citizen;c.title="Broken light";c.description="The light is broken outside the college";c.category="Streetlight";c.area="Ward A";c.department="Electrical maintenance";c.trackingNumber="CT-"+UUID.randomUUID();c.requestKey=UUID.randomUUID();return complaints.saveAndFlush(c);}
 @Test void unauthenticatedCallsAreDenied()throws Exception{mvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());}
 @Test void forgedMetadataCannotCreateStaff()throws Exception{UUID id=UUID.randomUUID();mvc.perform(get("/api/me").with(jwt().jwt(j->j.subject(id.toString()).claim("user_metadata",java.util.Map.of("role","ADMIN"))))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CITIZEN"));}
 @Test void privateDetailsAreScoped()throws Exception{Complaint c=report();mvc.perform(get("/api/complaints/"+c.id).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());mvc.perform(get("/api/complaints/"+c.id).with(jwt().jwt(j->j.subject(citizen.toString())))).andExpect(status().isOk());}
 @Test void assignmentAndConcurrentEdits()throws Exception{Complaint c=report();c.status=Status.ACKNOWLEDGED;complaints.saveAndFlush(c);long v=c.version;String body="{\"officerId\":\""+officer+"\",\"reason\":\"Assign local team\",\"version\":"+v+"}";mvc.perform(post("/api/complaints/"+c.id+"/assignment").with(jwt().jwt(j->j.subject(supervisor.toString()))).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED"));complaints.flush();mvc.perform(post("/api/complaints/"+c.id+"/assignment").with(jwt().jwt(j->j.subject(supervisor.toString()))).contentType("application/json").content(body)).andExpect(status().isConflict());assertEquals(1,events.findByComplaintIdOrderByCreatedAtAsc(c.id).size());}
 @Test void internalNotesNeverReachCitizen()throws Exception{Complaint c=report();StatusEvent e=new StatusEvent();e.complaintId=c.id;e.actorId=supervisor;e.actorName="Supervisor";e.action="COMMENT";e.message="Private staffing note";e.internal=true;events.saveAndFlush(e);mvc.perform(get("/api/complaints/"+c.id).with(jwt().jwt(j->j.subject(citizen.toString())))).andExpect(jsonPath("$.events").isEmpty());}
 @Test void retryDoesNotDuplicateSubmission()throws Exception{String body="{\"title\":\"Broken streetlight\",\"description\":\"Streetlight broken near college gate\",\"category\":\"Streetlight\",\"area\":\"Ward A\",\"landmark\":\"College\",\"latitude\":23.3,\"longitude\":85.3,\"urgency\":\"NORMAL\",\"requestKey\":\""+UUID.randomUUID()+"\"}";long before=complaints.count();for(int i=0;i<2;i++)mvc.perform(post("/api/complaints").with(jwt().jwt(j->j.subject(citizen.toString()))).contentType("application/json").content(body)).andExpect(status().isOk());assertEquals(before+1,complaints.count());}
 @Test void citizenCannotProvisionStaff()throws Exception{mvc.perform(put("/api/admin/staff/"+officer).with(jwt().jwt(j->j.subject(citizen.toString()))).contentType("application/json").content("{\"role\":\"ADMIN\",\"name\":\"Escalation\"}")).andExpect(status().isForbidden());}
 @Test void invalidLocationRejected()throws Exception{mvc.perform(post("/api/complaints").with(jwt().jwt(j->j.subject(citizen.toString()))).contentType("application/json").content("{\"title\":\"Broken streetlight\",\"description\":\"Streetlight broken near college gate\",\"category\":\"Streetlight\",\"area\":\"Ward A\",\"landmark\":\"College\",\"latitude\":123,\"longitude\":85,\"requestKey\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isBadRequest());}
 @Test void proposedResolutionRequiresEvidenceFromCurrentWorkCycle()throws Exception{
  Complaint c=report();c.status=Status.IN_PROGRESS;c.officerId=officer;c.workStartedAt=java.time.Instant.now();complaints.saveAndFlush(c);
  Attachment old=new Attachment();old.complaintId=c.id;old.uploadedBy=officer;old.kind="RESOLUTION";old.objectPath="old.jpg";old.createdAt=c.workStartedAt.minusSeconds(60);attachments.saveAndFlush(old);
  String body="{\"status\":\"RESOLUTION_PROPOSED\",\"reason\":\"Repair complete\",\"version\":"+c.version+"}";
  mvc.perform(post("/api/complaints/"+c.id+"/status").with(jwt().jwt(j->j.subject(officer.toString()))).contentType("application/json").content(body)).andExpect(status().isForbidden());
  Attachment fresh=new Attachment();fresh.complaintId=c.id;fresh.uploadedBy=officer;fresh.kind="RESOLUTION";fresh.objectPath="fresh.jpg";fresh.createdAt=c.workStartedAt.plusSeconds(1);attachments.saveAndFlush(fresh);
  mvc.perform(post("/api/complaints/"+c.id+"/status").with(jwt().jwt(j->j.subject(officer.toString()))).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLUTION_PROPOSED"));
  complaints.flush();
  mvc.perform(post("/api/complaints/"+c.id+"/status").with(jwt().jwt(j->j.subject(citizen.toString()))).contentType("application/json").content("{\"status\":\"CLOSED\",\"reason\":\"Verified the repair\",\"version\":"+c.version+"}")).andExpect(status().isOk());
  complaints.flush();
  mvc.perform(post("/api/complaints/"+c.id+"/status").with(jwt().jwt(j->j.subject(citizen.toString()))).contentType("application/json").content("{\"status\":\"REOPENED\",\"reason\":\"The light failed again\",\"version\":"+c.version+"}")).andExpect(status().isOk()).andExpect(jsonPath("$.closedAt").isEmpty());
  assertEquals(3,events.findByComplaintIdOrderByCreatedAtAsc(c.id).size());
 }
 @Test void disguisedUploadRejected()throws Exception{
  Complaint c=report();var file=new org.springframework.mock.web.MockMultipartFile("file","photo.jpg","image/jpeg","not an image".getBytes());
  mvc.perform(multipart("/api/complaints/"+c.id+"/attachments").file(file).param("kind","REPORT").with(jwt().jwt(j->j.subject(citizen.toString())))).andExpect(status().isBadRequest());
 }
}
