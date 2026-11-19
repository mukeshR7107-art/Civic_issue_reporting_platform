package in.civictrack;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface Profiles extends JpaRepository<Profile,UUID> { List<Profile> findByRole(Role role); }
interface Complaints extends JpaRepository<Complaint,UUID>, JpaSpecificationExecutor<Complaint> {
 Optional<Complaint> findByReporterIdAndRequestKey(UUID reporterId,UUID requestKey);
}
interface Events extends JpaRepository<StatusEvent,UUID> { List<StatusEvent> findByComplaintIdOrderByCreatedAtAsc(UUID id); }
interface Attachments extends JpaRepository<Attachment,UUID> { List<Attachment> findByComplaintId(UUID id); boolean existsByComplaintIdAndKindAndCreatedAtAfter(UUID id,String kind,java.time.Instant after); }
interface Notifications extends JpaRepository<Notification,UUID> { List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(UUID id); }
interface Routes extends JpaRepository<RoutingRule,UUID> { Optional<RoutingRule> findByCategoryAndArea(String category,String area); }
