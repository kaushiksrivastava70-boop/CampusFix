package com.campusfix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@RestController
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
@RequestMapping("/api")
public class CampusFixApplication {
  private final List<Issue> issues = new CopyOnWriteArrayList<>();
  private final AtomicLong ids = new AtomicLong(1004);
  private static final List<String> STATUSES = List.of("Reported", "In Progress", "Resolved");
  private static final List<String> PRIORITIES = List.of("Low", "Medium", "High", "Urgent");

  public CampusFixApplication() {
    issues.add(seed(1003, "Water cooler not working", "Water & Plumbing", "Block A · Ground Floor", "High", "The cooler is not dispensing water. Please inspect the power and water supply.", "In Progress", "Facilities Team", 4));
    issues.add(seed(1002, "Broken classroom lights", "Electrical", "Block B · Room 204", "Medium", "Two ceiling lights need replacement before the next lecture.", "Reported", "Unassigned", 1));
    issues.add(seed(1001, "Wi-Fi signal weak in library", "Network & Wi-Fi", "Central Library · Floor 2", "Low", "The connection keeps dropping during online research sessions.", "Resolved", "IT Support", 0));
    issues.add(seed(1000, "Washroom tap leaking", "Water & Plumbing", "Block C · First Floor", "Urgent", "Continuous leakage near the washbasin. Please check today.", "In Progress", "Plumbing Team", 2));
  }
  private Issue seed(long id, String title, String category, String location, String priority, String description, String status, String assignee, int hoursAgo) {
    Issue i = new Issue(id, title, category, location, priority, description, status, assignee);
    i.createdAt = Instant.now().minusSeconds(hoursAgo * 3600L).toString();
    i.updatedAt = i.createdAt;
    i.history.add(new HistoryEntry(status, "Issue created", i.createdAt));
    if (hoursAgo > 0) i.comments.add(new Comment("CampusFix Bot", "Initial report recorded. The campus team can review this ticket.", i.createdAt));
    return i;
  }
  public static void main(String[] args) { SpringApplication.run(CampusFixApplication.class, args); }

  @GetMapping("/health") public Map<String, Object> health() { return Map.of("status", "UP", "service", "CampusFix API", "time", Instant.now().toString()); }
  @GetMapping("/issues") public List<Issue> all() { return issues; }
  @GetMapping("/issues/{id}") public Issue one(@PathVariable long id) { return find(id); }

  @PostMapping("/issues") @ResponseStatus(HttpStatus.CREATED)
  public Issue create(@RequestBody IssueRequest r) {
    if (r.title == null || r.title.isBlank() || r.location == null || r.location.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and location are required");
    String priority = val(r.priority, "Medium");
    if (!PRIORITIES.contains(priority)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority");
    Issue i = new Issue(ids.getAndIncrement(), r.title.trim(), val(r.category, "Other"), r.location.trim(), priority, val(r.description, ""), "Reported", "Unassigned");
    i.createdAt = Instant.now().toString(); i.updatedAt = i.createdAt;
    i.history.add(new HistoryEntry("Reported", "Issue submitted", i.createdAt));
    issues.add(0, i); return i;
  }

  @PatchMapping("/issues/{id}") public Issue update(@PathVariable long id, @RequestBody Map<String,String> body) {
    Issue i = find(id); boolean changed = false;
    if (body.containsKey("status")) {
      String status = body.get("status");
      if (!STATUSES.contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
      if (!Objects.equals(i.status, status)) { i.status = status; i.history.add(new HistoryEntry(status, "Status changed to " + status, Instant.now().toString())); changed = true; }
    }
    if (body.containsKey("assignee")) { i.assignee = val(body.get("assignee"), "Unassigned"); changed = true; }
    if (body.containsKey("priority")) {
      String p = body.get("priority"); if (!PRIORITIES.contains(p)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority");
      if (!Objects.equals(i.priority, p)) { i.priority = p; changed = true; }
    }
    if (changed) i.updatedAt = Instant.now().toString();
    return i;
  }

  @PostMapping("/issues/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
  public Comment addComment(@PathVariable long id, @RequestBody CommentRequest r) {
    Issue i = find(id);
    if (r.text == null || r.text.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
    Comment c = new Comment(val(r.author, "Campus Staff"), r.text.trim(), Instant.now().toString());
    i.comments.add(c); i.updatedAt = c.createdAt; return c;
  }

  @DeleteMapping("/issues/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) { Issue i = find(id); issues.remove(i); }

  @GetMapping("/stats") public Map<String,Object> stats() {
    long total = issues.size(), resolved = issues.stream().filter(i -> "Resolved".equals(i.status)).count();
    Map<String,Long> byStatus = new LinkedHashMap<>(), byCategory = new LinkedHashMap<>(), byPriority = new LinkedHashMap<>();
    for (String s : STATUSES) byStatus.put(s, issues.stream().filter(i -> s.equals(i.status)).count());
    for (Issue i : issues) { byCategory.merge(i.category, 1L, Long::sum); byPriority.merge(i.priority, 1L, Long::sum); }
    return Map.of("total", total, "resolved", resolved, "resolutionRate", total == 0 ? 0 : Math.round(resolved * 100.0 / total), "byStatus", byStatus, "byCategory", byCategory, "byPriority", byPriority);
  }

  private Issue find(long id) { return issues.stream().filter(x -> x.id == id).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found")); }
  private String val(String s, String fallback) { return s == null || s.isBlank() ? fallback : s.trim(); }
  public static class IssueRequest { public String title, category, location, priority, description; }
  public static class CommentRequest { public String text, author; }
  public static class Issue {
    public long id; public String title, category, location, priority, description, status, assignee, createdAt, updatedAt;
    public List<Comment> comments = new CopyOnWriteArrayList<>(); public List<HistoryEntry> history = new CopyOnWriteArrayList<>();
    public Issue(long id, String title, String category, String location, String priority, String description, String status, String assignee) {
      this.id=id; this.title=title; this.category=category; this.location=location; this.priority=priority; this.description=description; this.status=status; this.assignee=assignee;
      this.createdAt=Instant.now().toString(); this.updatedAt=this.createdAt;
    }
  }
  public static class Comment { public String author, text, createdAt; public Comment(String a,String t,String c){author=a;text=t;createdAt=c;} }
  public static class HistoryEntry { public String status, message, createdAt; public HistoryEntry(String s,String m,String c){status=s;message=m;createdAt=c;} }
}
