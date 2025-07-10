package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.repositories.HyperParameterRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.services.ChatService;
import com.backend.softtrainer.services.EnhancedMessageProcessor;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.HybridAiProcessingService;
import com.backend.softtrainer.services.SkillService;
import com.backend.softtrainer.services.UserMessageService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chats")
@AllArgsConstructor
@Slf4j
public class ChatController {

  private final ChatService chatService;

  private final FlowService flowService;

  private final UserMessageService userMessageService;

  private final UserHyperParameterRepository userHyperParameterRepository;

  private final HyperParameterRepository hyperParameterRepository;

  private final CustomUsrDetailsService customUsrDetailsService;

  private final SimulationRepository simulationRepository;

  private final SkillService skillService;

  private final ApplicationEventPublisher eventPublisher;

  private final EnhancedMessageProcessor enhancedMessageProcessor;

  private final HybridAiProcessingService hybridAiProcessingService;

  private final DualModeSimulationRuntime dualModeSimulationRuntime;

  @PutMapping("/create")
  @PreAuthorize("@customUsrDetailsService.isSimulationAvailable(authentication, #chatRequestDto.simulationId)")
  public ResponseEntity<ChatResponseDto> create(@RequestBody ChatRequestDto chatRequestDto, Authentication authentication) {

    var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());

    var simulationOpt = simulationRepository.findById(chatRequestDto.getSimulationId());

    if (simulationOpt.isPresent()) {
      var simulation = simulationOpt.get();

      if (userDetails.user().getRoles().stream().noneMatch(a -> a.getName().equals(StaticRole.ROLE_USER))) {
        if (chatService.existsBy(userDetails.user(), chatRequestDto.getSimulationId())) {
          return ResponseEntity.ok(new ChatResponseDto(
            null,
            null,
            false,
            String.format(
              "Chat already exists for user %s and for training %s",
              userDetails.user().getId(),
              chatRequestDto.getSimulationId()
            ),
            null,
            new ChatParams(null)
          ));
        }
      }

      try {
        var createdChat = chatService.store(simulation, userDetails.user());

        // 🚀 Use new dual-mode runtime for initialization
        var initialMessages = dualModeSimulationRuntime.initializeChat(createdChat).get();

        var chatParams = new ChatParams(createdChat.getHearts());
        var combinedMessages = userMessageService.combineMessages(initialMessages, chatParams);

        //init default values to the hyper params for the user
        initUserDefaultHyperParams(simulation.getId(), createdChat.getId(), userDetails.user().getId());

        log.info(
          "Chat {} created for user {} and simulation {} using dual-mode runtime",
          createdChat.getId(),
          userDetails.user().getId(),
          simulation.getId()
        );

        return ResponseEntity.ok(new ChatResponseDto(
          createdChat.getId(),
          chatRequestDto.getSkillId(),
          true,
          "success",
          combinedMessages,
          chatParams
        ));

      } catch (Exception e) {
        log.error("❌ Error creating chat with dual-mode runtime", e);
        return ResponseEntity.ok(new ChatResponseDto(
          null,
          chatRequestDto.getSkillId(),
          false,
          String.format("Failed to create chat for simulation %s: %s", chatRequestDto.getSimulationId(), e.getMessage()),
          null,
          new ChatParams(null)
        ));
      }
    } else {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        null,
        false,
        String.format("No simulation %s", chatRequestDto.getSimulationId()),
        null,
        new ChatParams(null)
      ));
    }
  }

  //todo rename it
  @GetMapping("/get")
  @PreAuthorize("@customUsrDetailsService.isSimulationAvailable(authentication, #simulationId)")
  public ResponseEntity<ChatResponseDto> getUserChatBySimulation(@RequestParam(name = "simulationId") Long simulationId,
                                                                 Authentication authentication) {

    var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());
    var chatOptional = chatService.findChatWithMessages(userDetails.user(), simulationId);

    if (chatOptional.isEmpty()) {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        null,
        false,
        String.format(
          "Chat doesn't exists for user %s and for simulation %s",
          authentication.getName(),
          simulationId
        ),
        null,
        new ChatParams(null)
      ));
    }

    var chat = chatOptional.get();
    var messages = chat.getMessages().stream().toList();

    var chatParams = new ChatParams(chat.getHearts());

    var combinedMessages = userMessageService.combineMessages(messages, chatParams);
    return ResponseEntity.ok(new ChatResponseDto(
      chat.getId(),
      null,
      true,
      "success",
      combinedMessages,
      chatParams
    ));
  }

  @GetMapping("/get/by")
  @PreAuthorize("@customUsrDetailsService.isChatOfUser(authentication, #chatId)")
  public ResponseEntity<ChatResponseDto> getUserChatById(@RequestParam(name = "chatId") Long chatId,
                                                         Authentication authentication) {

    var chatOptional = chatService.findChatWithMessages(chatId);

    if (chatOptional.isEmpty()) {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        null,
        false,
        String.format(
          "Chat % doesn't exists for user %s",
          chatId,
          authentication.getName()
        ),
        null,
        new ChatParams(null)
      ));
    }

    var chat = chatOptional.get();
    var messages = chat.getMessages().stream().toList();

    var chatParams = new ChatParams(chat.getHearts());

    var combinedMessages = userMessageService.combineMessages(messages, chatParams);
    return ResponseEntity.ok(new ChatResponseDto(
      chat.getId(),
      null,
      true,
      "success",
      combinedMessages,
      chatParams
    ));
  }


//  @GetMapping("/get/all")
//  @PreAuthorize("@customUsrDetailsService.isResourceOwner(authentication, #ownerId)")
//  public ResponseEntity<ChatsResponseDto> getAll(@RequestParam(name = "ownerId") Long ownerId) {
//    var chats = chatService.getAll(ownerId);
//    return ResponseEntity.ok(new ChatsResponseDto(
//      chats.stream().map(Chat::getSimulationName).toList(),
//      true,
//      "success"
//    ));
//  }

  private void initUserDefaultHyperParams(final Long simulationId, final Long chatId, final Long userId) {
    var hyperparams = hyperParameterRepository.getAllKeysBySimulationId(simulationId);

    var userHyperParams = hyperparams.stream()
      .map(hpKey -> UserHyperParameter.builder()
        .key(hpKey)
        .chatId(chatId)
        .ownerId(userId)
        .simulationId(simulationId)
        .value((double) 0)
        .build())
      .toList();

    userHyperParameterRepository.saveAll(userHyperParams);
    log.info("User hyper params {} initialized for user {} and simulation {}", hyperparams, userId, simulationId);
    // Publish event for cache eviction
    if (userId != null) {
        String userEmail = userHyperParameterRepository.findUserEmailById(userId);
        if (userEmail != null) {
            eventPublisher.publishEvent(new com.backend.softtrainer.events.HyperParameterUpdatedEvent(userEmail));
            log.info("Published hyperparameter update event for user: {} (bulk insert)", userEmail);
        }
    }
  }
}
