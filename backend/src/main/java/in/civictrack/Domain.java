package in.civictrack;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

enum Role { CITIZEN, SUPERVISOR, OFFICER, ADMIN }
enum Status { SUBMITTED, NEEDS_INFORMATION, ACKNOWLEDGED, ASSIGNED, IN_PROGRESS, RESOLUTION_PROPOSED, CLOSED, REOPENED, REJECTED }
enum Priority { LOW, NORMAL, HIGH, CRITICAL }

@Entity @Table(name="profiles")
class Profile {
 @Id public UUID id;
 public String name;
 @Enumerated(EnumType.STRING) public Role role = Role.CITIZEN;
 public String department;
 public String area;
 protected Profile() {}
 Profile(UUID id, String name) { this.id=id; this.name=name; }
}

@Entity @Table(name="complaints", uniqueConstraints=@UniqueConstraint(columnNames={"reporter_id","request_key"}))
class Complaint {
 @Id public UUID id = UUID.randomUUID();
 @Version public long version;
 @Column(unique=true,nullable=false) public String trackingNumber;
 @Column(nullable=false) public UUID reporterId;
 public String title;
 @Column(length=4000) public String description;
 public String category;
 public String area;
 public String landmark;
 public double latitude;
 public double longitude;
 public String department;
 public UUID officerId;
 @Enumerated(EnumType.STRING) public Status status = Status.SUBMITTED;
 @Enumerated(EnumType.STRING) public Priority priority = Priority.NORMAL;
 public String urgency;
 public UUID requestKey;
 public Instant createdAt = Instant.now();
 public Instant updatedAt = Instant.now();
 public Instant dueAt = Instant.now().plusSeconds(72*3600);
 public Instant proposedAt;
 public Instant workStartedAt;
 public Instant closedAt;
 protected Complaint() {}
}

@Entity @Table(name="status_events")
class StatusEvent {
 @Id public UUID id = UUID.randomUUID();
 public UUID complaintId;
 public UUID actorId;
 public String actorName;
 public String action;
 @Column(length=4000) public String message;
 public boolean internal;
 public Instant createdAt = Instant.now();
 protected StatusEvent() {}
}

@Entity @Table(name="attachments")
class Attachment {
 @Id public UUID id=UUID.randomUUID();
 public UUID complaintId;
 public UUID uploadedBy;
 public String objectPath;
 public String kind;
 public String contentType;
 public Instant createdAt=Instant.now();
 protected Attachment() {}
}

@Entity @Table(name="notifications")
class Notification {
 @Id public UUID id=UUID.randomUUID();
 public UUID userId;
 public UUID complaintId;
 @Column(length=4000) public String message;
 public boolean seen;
 public Instant createdAt=Instant.now();
 protected Notification() {}
}

@Entity @Table(name="routing_rules")
class RoutingRule {
 @Id public UUID id=UUID.randomUUID();
 public String category;
 public String area;
 public String department;
 protected RoutingRule() {}
}
