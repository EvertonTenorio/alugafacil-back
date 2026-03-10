package br.edu.ufape.alugafacil.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationRequest;
import br.edu.ufape.alugafacil.dtos.notifications.MessageNotificationResponse;
import br.edu.ufape.alugafacil.models.Conversation;
import br.edu.ufape.alugafacil.models.MessageNotification;
import br.edu.ufape.alugafacil.models.Notification;
import br.edu.ufape.alugafacil.models.Property;
import br.edu.ufape.alugafacil.models.User;
import br.edu.ufape.alugafacil.repositories.ConversationRepository;
import br.edu.ufape.alugafacil.repositories.NotificationRepository;
import br.edu.ufape.alugafacil.repositories.PropertyRepository;
import br.edu.ufape.alugafacil.repositories.UserRepository;
import br.edu.ufape.alugafacil.services.FCMService;
import br.edu.ufape.alugafacil.services.NotificationService;
import br.edu.ufape.alugafacil.services.interfaces.IFileStorageService;
import br.edu.ufape.alugafacil.services.interfaces.IPropertyService;

@SpringBootTest
@ActiveProfiles("test")
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ConversationRepository conversationRepository;

    @MockBean
    private FCMService fcmService;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private IPropertyService propertyService;

    @Test
    @DisplayName("Deve persistir uma notificação de mensagem no banco H2")
    void shouldPersistMessageNotificationInDatabase() {
        User sender = new User();
        sender.setEmail("sender@test.com");
        userRepository.save(sender);

        User recipient = new User();
        recipient.setEmail("recipient@test.com");
        recipient.setFcmToken("token-fake-123");
        userRepository.save(recipient);

        Property property = new Property();
        property.setOwner(recipient);
        propertyRepository.save(property);

        Conversation conversation = new Conversation();
        conversation.setInitiatorUser(sender);
        conversation.setRecipientUser(recipient);
        conversation.setProperty(property);
        Conversation savedConversation = conversationRepository.save(conversation);

        MessageNotificationRequest request = new MessageNotificationRequest();
        request.setConversationId(savedConversation.getConversationId());
        request.setSenderName("João da Silva");
        request.setTargetToken("token-fake-123");

        MessageNotificationResponse response = notificationService.createMessageNotification(request);

        assertNotNull(response.getId(), "O ID da resposta não deveria ser nulo");
        
        Notification saved = notificationRepository.findById(response.getId()).orElse(null);
        
        assertNotNull(saved, "A notificação deveria ter sido encontrada no banco de dados");
        assertTrue(saved instanceof MessageNotification, "Deveria ser uma instância de MessageNotification");
        assertEquals("João da Silva", ((MessageNotification) saved).getSenderName());
        
        if (saved.getUser() != null) {
             assertEquals(recipient.getUserId(), saved.getUser().getUserId());
        }
    }
}