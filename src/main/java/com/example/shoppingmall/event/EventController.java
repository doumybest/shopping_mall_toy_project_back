package com.example.shoppingmall.event;

import com.example.shoppingmall.auth.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class EventController {
    private final EventService events;
    private final MemberService members;

    public EventController(EventService events, MemberService members) {
        this.events = events;
        this.members = members;
    }

    @GetMapping("/api/events")
    List<EventService.Banner> active() { return events.active(); }

    @GetMapping("/api/events/{id}")
    EventService.Banner detail(@PathVariable long id) { return events.detail(id); }

    @GetMapping("/api/events/{id}/image")
    ResponseEntity<byte[]> image(@PathVariable long id) {
        var image = events.image(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.mediaType()))
            .cacheControl(CacheControl.noStore())
            .body(image.bytes());
    }

    @GetMapping("/api/coupons/claimed-events")
    List<Long> claimed(Authentication auth) { return events.claimedEventIds(members.current(auth).id()); }

    @PostMapping("/api/events/{id}/coupon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void claim(Authentication auth, @PathVariable long id) {
        events.claim(members.current(auth).id(), id);
    }

    @GetMapping("/api/coupons")
    List<EventService.Coupon> coupons(Authentication auth) {
        return events.coupons(members.current(auth).id());
    }

    @GetMapping("/api/admin/events")
    List<EventService.Event> list() { return events.list(); }

    @PostMapping("/api/admin/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void create(@Valid @RequestBody EventService.EventInput input) { events.save(null, input); }

    @DeleteMapping("/api/admin/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) { events.delete(id); }

    @PutMapping("/api/admin/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable long id, @Valid @RequestBody EventService.EventInput input) {
        events.save(id, input);
    }
}
