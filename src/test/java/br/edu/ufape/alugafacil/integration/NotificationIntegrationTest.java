package br.edu.ufape.alugafacil.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional // <-- Garante o rollback do banco e o controle de transação após cada teste
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
        
        // 1. Reatribuindo o retorno do save para garantir que o 'sender' tenha um ID gerado
        User sender = new User();
        sender.setEmail("sender@test.com");
        sender.setName("João da Silva"); // Adicionado para consistência lógica
        sender = userRepository.save(sender);

        User recipient = new User();
        recipient.setEmail("recipient@test.com");
        recipient.setFcmToken("token-fake-123");
        recipient = userRepository.save(recipient);

        Property property = new Property();
        // property.setUser(recipient);
        // NOTA: Se Property tiver campos obrigatórios (ex: title, price), preencha-os aqui para não dar erro de validação (ConstraintViolation)
        property = propertyRepository.save(property);

        Conversation conversation = new Conversation();
        conversation.setInitiatorUser(sender);
        conversation.setRecipientUser(recipient);
        conversation.setProperty(property);
        Conversation savedConversation = conversationRepository.save(conversation);

        // 2. Usando os dados das entidades salvas para o Request
        MessageNotificationRequest request = new MessageNotificationRequest();
        request.setConversationId(savedConversation.getConversationId());
        request.setSenderName(sender.getName());
        request.setTargetToken(recipient.getFcmToken());

        // Ação
        MessageNotificationResponse response = notificationService.createMessageNotification(request);

        // Verificações
        assertNotNull(response.getId(), "O ID da resposta não deveria ser nulo");
        
        Notification saved = notificationRepository.findById(response.getId()).orElse(null);
        
        assertNotNull(saved, "A notificação deveria ter sido encontrada no banco de dados");
        assertTrue(saved instanceof MessageNotification, "Deveria ser uma instância de MessageNotification");
        assertEquals("João da Silva", ((MessageNotification) saved).getSenderName());
        
        // Verificação final otimizada para evitar NullPointerException no assert
        assertNotNull(saved.getUser(), "O usuário atrelado à notificação não deve ser nulo");
        assertEquals(recipient.getUserId(), saved.getUser().getUserId(), "A notificação deve pertencer ao destinatário (recipient) correto");
    }
}