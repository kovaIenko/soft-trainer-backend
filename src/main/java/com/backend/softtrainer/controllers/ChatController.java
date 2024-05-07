package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ChatRequestDto;
import com.backend.softtrainer.dtos.ChatResponseDto;
import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.repositories.HyperParameterRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import com.backend.softtrainer.services.ChatService;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.SkillService;
import com.backend.softtrainer.services.UserMessageService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final MessageService messageService;

  private final UserMessageService userMessageService;

  private final UserHyperParameterRepository userHyperParameterRepository;

  private final HyperParameterRepository hyperParameterRepository;

  private final CustomUsrDetailsService customUsrDetailsService;

  private final SimulationRepository simulationRepository;

  private final SkillService skillService;

  @PutMapping("/create")
  @PreAuthorize("@customUsrDetailsService.areSkillAndSimulationAvailable(authentication, #chatRequestDto.skillId, " +
    "#chatRequestDto.simulationId)")
  public ResponseEntity<ChatResponseDto> create(@RequestBody ChatRequestDto chatRequestDto, Authentication authentication) {

    var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());

    var simulationOpt = simulationRepository.findById(chatRequestDto.getSimulationId());

    if (simulationOpt.isPresent()) {
      var simulation = simulationOpt.get();

      if (!skillService.isSimulationAvailableForUser(userDetails.user(), simulation)) {
        return ResponseEntity.ok(new ChatResponseDto(
          null,
          chatRequestDto.getSkillId(),
          false,
          String.format(
            "Simulation %s is not available",
            chatRequestDto.getSimulationId()
          ),
          null
        ));
      }
      if (userDetails.user().getRoles().stream().noneMatch(a -> a.getName().equals(StaticRole.ROLE_OWNER))) {
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
            null
          ));
        }
      }

      var flowTillActions = flowService.getFirstFlowNodesUntilActionable(chatRequestDto.getSimulationId());

      if (!flowTillActions.isEmpty()) {
        var createdChat = chatService.store(simulation, userDetails.user());

        var messages = messageService.getAndStoreMessageByFlow(flowTillActions, createdChat).stream().toList();
        var combinedMessages = userMessageService.combineMessages(messages);

        var hyperparams = hyperParameterRepository.getAllKeysByFlowName(simulation.getName())
          .stream()
          .map(hpKey -> UserHyperParameter.builder()
            .key(hpKey)
            .chatId(createdChat.getId())
            .ownerId(userDetails.user().getId())
            .value((double) 0)
            .build())
          .toList();

        userHyperParameterRepository.saveAll(hyperparams);

        return ResponseEntity.ok(new ChatResponseDto(
          createdChat.getId(),
          chatRequestDto.getSkillId(),
          true,
          "success",
          combinedMessages
        ));

      } else {
        return ResponseEntity.ok(new ChatResponseDto(
          null,
          chatRequestDto.getSkillId(),
          false,
          String.format("The is no flow nodes to display for simulation %s", chatRequestDto.getSimulationId()),
          null
        ));
      }
    } else {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        null,
        false,
        String.format("No simulation %s", chatRequestDto.getSimulationId()),
        null
      ));
    }
  }

  @GetMapping("/get")
  @PreAuthorize("@customUsrDetailsService.isSimulationAvailable(authentication, #simulationId)")
  public ResponseEntity<ChatResponseDto> getUserChatBySimulation(@RequestParam(name = "simulationId") Long simulationId,
                                                                 Authentication authentication) {

    var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());

    var simulationOpt = simulationRepository.findById(simulationId);
    if (simulationOpt.isPresent()) {
      var simulation = simulationOpt.get();
      var chatOptional = chatService.findChatWithMessages(userDetails.user(), simulation);

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
          null
        ));
      }

      var chat = chatOptional.get();

      var messages = chat.getMessages().stream().toList();

      var combinedMessages = userMessageService.combineMessages(messages);

      return ResponseEntity.ok(new ChatResponseDto(
        chat.getId(),
        null,
        true,
        "success",
        combinedMessages
      ));
    } else {
      return ResponseEntity.ok(new ChatResponseDto(
        null,
        null,
        false,
        String.format("The is no such simulation %s", simulationId),
        null
      ));
    }
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

}
