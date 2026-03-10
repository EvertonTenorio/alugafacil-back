package br.edu.ufape.alugafacil.controllers;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.NotificationResponse;
import br.edu.ufape.alugafacil.services.interfaces.INotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final INotificationService notificationService;

    @PostMapping("/listing")
    public ResponseEntity<ListingNotificationResponse> createListingNotification(@Valid @RequestBody ListingNotificationRequest request) {
        ListingNotificationResponse response = notificationService.createListingNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<NotificationResponse>> listByUser(
            @PathVariable UUID userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable pageable
    ) {
        Page<NotificationResponse> response = notificationService.getNotificationsByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/message")
    public ResponseEntity<MessageNotificationResponse> createMessageNotification(@Valid @RequestBody MessageNotificationRequest request) {
        MessageNotificationResponse response = notificationService.createMessageNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> listAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, page = 0, size = 10) Pageable pageable
    ) {
        Page<NotificationResponse> response = notificationService.getAllNotifications(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(@PathVariable UUID id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllByUser(@PathVariable UUID userId) {
        notificationService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}