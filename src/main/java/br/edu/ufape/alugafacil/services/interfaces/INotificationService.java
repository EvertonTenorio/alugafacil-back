package br.edu.ufape.alugafacil.services.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.NotificationResponse;

public interface INotificationService {
    
    // Métodos chamados pelo Controller (via API/Frontend)
    ListingNotificationResponse createListingNotification(ListingNotificationRequest request);
    MessageNotificationResponse createMessageNotification(MessageNotificationRequest request);
    
    
    void notifyListingMatch(UUID userId, UUID propertyId, String alertName);
    void notifyNewMessage(UUID recipientId, UUID conversationId, String senderName);
    Page<NotificationResponse> getNotificationsByUserId(UUID userId, Pageable pageable);

    Page<NotificationResponse> getAllNotifications(Pageable pageable);
    NotificationResponse getNotificationById(UUID id);
    NotificationResponse markAsRead(UUID id);
    void deleteNotification(UUID id);
}