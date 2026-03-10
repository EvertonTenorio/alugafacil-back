package br.edu.ufape.alugafacil.services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.ListingNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationResponse;
import br.edu.ufape.alugafacil.dtos.notifications.NotificationResponse;
import br.edu.ufape.alugafacil.exceptions.ResourceNotFoundException; // <--- Import da sua exceção
import br.edu.ufape.alugafacil.mappers.NotificationMapper;
import br.edu.ufape.alugafacil.models.ListingNotification;
import br.edu.ufape.alugafacil.models.MessageNotification;
import br.edu.ufape.alugafacil.models.Notification;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.repositories.ConversationRepository; // <--- Certifique-se de ter este repo
import br.edu.ufape.alugafacil.repositories.NotificationRepository;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;     // <--- E este
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.services.interfaces.INotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final FCMService fcmService;
    private final UserRepository userRepository;
    
    
    private final PropertyRepository propertyRepository;
    private final ConversationRepository conversationRepository; 

    @Override
    @Transactional
    public ListingNotificationResponse createListingNotification(ListingNotificationRequest request) {
        // 1. VALIDAÇÃO: Verifica se o imóvel realmente existe
        if (!propertyRepository.existsById(request.getPropertyId())) {
            throw new ResourceNotFoundException("Imóvel não encontrado com ID: " + request.getPropertyId());
        }

        ListingNotification notification = notificationMapper.toEntity(request);
        ListingNotification saved = notificationRepository.save(notification);

        if (request.getTargetToken() != null && !request.getTargetToken().isEmpty()) {
            sendListingPush(request.getTargetToken(), saved, request.getPropertyId());
        }

        return notificationMapper.toResponse(saved);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByUserId(UUID userId, Pageable pageable) {
        
        return notificationRepository.findByUserId(userId, pageable)
                .map(notificationMapper::toResponse); 
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public MessageNotificationResponse createMessageNotification(MessageNotificationRequest request) {
        // 2. VALIDAÇÃO: Verifica se a conversa existe (se o ID foi passado)
        if (request.getConversationId() != null && !conversationRepository.existsById(request.getConversationId())) {
             throw new ResourceNotFoundException("Conversa não encontrada com ID: " + request.getConversationId());
        }

        MessageNotification notification = notificationMapper.toEntity(request);
        MessageNotification saved = notificationRepository.save(notification);

        if (request.getTargetToken() != null && !request.getTargetToken().isEmpty()) {
            sendMessagePush(request.getTargetToken(), saved, request.getConversationId(), request.getSenderName());
        }

        return notificationMapper.toResponse(saved);
    }

    // --- MÉTODOS INTERNOS (TRIGGERS) ---

    @Override
    @Transactional
    public void notifyListingMatch(UUID userId, UUID propertyId, String alertName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));

        ListingNotificationRequest request = new ListingNotificationRequest();
        request.setPropertyId(propertyId);
        request.setAlertName(alertName);

        ListingNotification notification = notificationMapper.toEntity(request);
        notification.setUser(user);

        ListingNotification saved = notificationRepository.save(notification);

        if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
            sendListingPush(user.getFcmToken(), saved, propertyId);
        }
    }

    @Override
    @Transactional
    public void notifyNewMessage(UUID recipientId, UUID conversationId, String senderName) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário destinatário não encontrado com ID: " + recipientId));

        MessageNotificationRequest request = new MessageNotificationRequest();
        request.setConversationId(conversationId);
        request.setSenderName(senderName);

        MessageNotification notification = notificationMapper.toEntity(request);
        notification.setUser(recipient);

        MessageNotification saved = notificationRepository.save(notification);

        if (recipient.getFcmToken() != null && !recipient.getFcmToken().isEmpty()) {
            sendMessagePush(recipient.getFcmToken(), saved, conversationId, senderName);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void sendListingPush(String token, ListingNotification notification, UUID propertyId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "LISTING");
        data.put("notificationId", notification.getNotificationId().toString());
        data.put("propertyId", propertyId.toString());

        fcmService.sendNotification(token, notification.getTitle(), notification.getMessage(), data);
    }

    private void sendMessagePush(String token, MessageNotification notification, UUID conversationId, String senderName) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "MESSAGE");
        data.put("notificationId", notification.getNotificationId().toString());
        if (conversationId != null) {
            data.put("conversationId", conversationId.toString());
        }
        data.put("senderName", senderName);

        fcmService.sendNotification(token, notification.getTitle(), notification.getMessage(), data);
    }

    // --- LEITURA ---

    @Override
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable)
                .map(notificationMapper::toResponse);
    }

    @Override
    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com ID: " + id));
        
        return notificationMapper.toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com ID: " + id));
        
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        
        return notificationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notificação não encontrada com ID: " + id);
        }
        notificationRepository.deleteById(id);
    }
}