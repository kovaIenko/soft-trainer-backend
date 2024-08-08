package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.CharacterDto;
import com.backend.softtrainer.dtos.SimulationRequestDto;
import com.backend.softtrainer.dtos.flow.EnterTextQuestionDto;
import com.backend.softtrainer.dtos.flow.FlowNodeDto;
import com.backend.softtrainer.dtos.flow.HintMessageDto;
import com.backend.softtrainer.dtos.flow.ImagesDto;
import com.backend.softtrainer.dtos.flow.MultiChoiceTaskDto;
import com.backend.softtrainer.dtos.flow.ResultSimulationDto;
import com.backend.softtrainer.dtos.flow.SingleChoiceQuestionDto;
import com.backend.softtrainer.dtos.flow.SingleChoiceTaskDto;
import com.backend.softtrainer.dtos.flow.TextDto;
import com.backend.softtrainer.dtos.flow.VideosDto;
import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.HyperParameter;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.SimulationComplexity;
import com.backend.softtrainer.entities.flow.ContentQuestion;
import com.backend.softtrainer.entities.flow.EnterTextQuestion;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.HintMessageNode;
import com.backend.softtrainer.entities.flow.MultipleChoiceTask;
import com.backend.softtrainer.entities.flow.ResultSimulationNode;
import com.backend.softtrainer.entities.flow.SingleChoiceQuestion;
import com.backend.softtrainer.entities.flow.SingleChoiceTask;
import com.backend.softtrainer.entities.flow.Text;
import com.backend.softtrainer.repositories.CharacterRepository;
import com.backend.softtrainer.repositories.FlowRepository;
import com.backend.softtrainer.repositories.HyperParameterRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FlowService {

  private final FlowRepository flowRepository;

  private final CharacterRepository characterRepository;

  private final HyperParameterRepository hyperParameterRepository;

  private final OrganizationRepository organizationRepository;

  private final SkillRepository skillRepository;

  private final SimulationRepository simulationRepository;

  public void uploadFlow(final SimulationRequestDto flowRequestDto) {
    var skillReq = flowRequestDto.getSkill();
    Skill temp = null;
    if (Objects.isNull(skillReq)) {
      throw new NoSuchElementException("New skill should be specified");
    }
    if (Objects.nonNull(skillReq.skillId())) {
      var optSkill = skillRepository.findById(skillReq.skillId());
      if (optSkill.isEmpty()) {
        throw new NoSuchElementException(String.format("There is no such skill with id %s", skillReq.skillId()));
      } else {
        temp = optSkill.get();
      }
    } else {
      if (Objects.isNull(skillReq.name()) || skillReq.name().isEmpty()) {
        throw new NoSuchElementException("New skill should contains of name");
      }
      temp = Skill.builder()
        .avatar(skillReq.avatar())
        .name(skillReq.name())
        .build();
    }

    var simulation = Simulation.builder()
      .complexity(SimulationComplexity.MEDIUM)
      .isOpen(true)
      .name(flowRequestDto.getName())
      .build();

    simulationRepository.save(simulation);

    Map<Long, Character> characterMap = flowRequestDto.getCharacters().stream()
      .collect(Collectors.toMap(
        CharacterDto::id, // Key extractor
        characterDto -> Character.builder()
          .name(characterDto.name())
          .avatar(characterDto.avatar())
          .flowCharacterId(characterDto.id())
          .build(), // Value function
        (existing, replacement) -> existing, // Merge function, in case of duplicate keys
        HashMap::new // Map supplier
      ));

    characterRepository.saveAll(characterMap.values());

    if (Objects.nonNull(flowRequestDto.getHyperparameters())) {
      List<HyperParameter> hyperParameters = flowRequestDto.getHyperparameters()
        .stream()
        .map(param -> HyperParameter.builder()
          .key(param.key())
          .simulationId(simulation.getId())
          .build())
        .toList();

      hyperParameterRepository.saveAll(hyperParameters);
    }

    var flowRecords =
      flowRequestDto.getFlow()
        .stream()
        .flatMap(flowNodeDto -> this.convert(flowNodeDto, characterMap.get(flowNodeDto.getAuthor()), simulation))
        .toList();

    var nodes = flowRepository.saveAll(flowRecords);

    if (!nodes.isEmpty()) {
      if (Objects.isNull(temp.getSimulations())) {
        temp.setSimulations(new HashMap<>());
      }
      temp.getSimulations().put(simulation, temp.getSimulations().keySet().size() + 1L);
      temp = skillRepository.save(temp);
    }

    simulation.setNodes(nodes);

    simulation.setSkill(temp);
    simulationRepository.save(simulation);
  }

  public Optional<FlowNode> findById(final Long simulationId) {
    return flowRepository.findById(simulationId);
  }

  public boolean isLastNode(final FlowNode flowNode) {
    return flowRepository.findAllBySimulationIdAndPreviousOrderNumber(flowNode.getSimulation().getId(), flowNode.getOrderNumber())
      .isEmpty();
  }

  private Stream<FlowNode> convert(final FlowNodeDto flowRecordDto,
                                   final Character authorEntity,
                                   final Simulation simulation) {

    if (flowRecordDto.getPreviousOrderNumber().isEmpty()) {
      return Stream.of(convertFlow(flowRecordDto, -1, authorEntity, simulation));
    }

    return flowRecordDto.getPreviousOrderNumber()
      .stream()
      .map(prevOrderNumber -> convertFlow(flowRecordDto, prevOrderNumber, authorEntity, simulation));
  }

  private FlowNode convertFlow(final FlowNodeDto flowRecordDto,
                               final long previousMessageId,
                               final Character authorEntity,
                               final Simulation simulation) {

    if (flowRecordDto instanceof ImagesDto imagesDto) {
      return ContentQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .url(imagesDto.getUrl())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.IMAGES)
        .simulation(simulation)
        .build();
    }
    if (flowRecordDto instanceof VideosDto videosDto) {
      return ContentQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .url(videosDto.getUrl())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.VIDEOS)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof EnterTextQuestionDto enterTextQuestionDto) {
      return EnterTextQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .prompt(enterTextQuestionDto.getPrompt())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.ENTER_TEXT_QUESTION)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof TextDto textDto) {
      return Text.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .text(textDto.getText())
        .character(authorEntity)
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.TEXT)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof SingleChoiceTaskDto singleChoiceTaskDto) {
      return SingleChoiceTask.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .correct(singleChoiceTaskDto.getCorrect())
        .options(String.join(" || ", singleChoiceTaskDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.SINGLE_CHOICE_TASK)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof SingleChoiceQuestionDto singleChoiceQuestionDto) {
      return SingleChoiceQuestion.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .correct(singleChoiceQuestionDto.getCorrect())
        .options(String.join(" || ", singleChoiceQuestionDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.SINGLE_CHOICE_QUESTION)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof MultiChoiceTaskDto multipleChoiceQuestionDto) {
      return MultipleChoiceTask.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .correct(String.join(" || ", multipleChoiceQuestionDto.getCorrect()))
        .options(String.join(" || ", multipleChoiceQuestionDto.getOptions()))
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.MULTI_CHOICE_TASK)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof HintMessageDto hintMessageDto) {
      return HintMessageNode.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .prompt(hintMessageDto.getPrompt())
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.HINT_MESSAGE)
        .simulation(simulation)
        .build();
    } else if (flowRecordDto instanceof ResultSimulationDto resultSimulationDto) {
      return ResultSimulationNode.builder()
        .orderNumber(flowRecordDto.getMessageId())
        .showPredicate(flowRecordDto.getShowPredicate())
        .character(authorEntity)
        .prompt(resultSimulationDto.getPrompt())
        .previousOrderNumber(previousMessageId)
        .messageType(MessageType.RESULT_SIMULATION)
        .simulation(simulation)
        .build();
    }
    throw new NoSuchElementException("There is no such type of message type");
  }

  public List<FlowNode> getFirstFlowNodesUntilActionable(final Long simulationId) {

    var actionableMessageTypes = MessageType.getActionableMessageTypes();

    List<FlowNode> questions = flowRepository.findFirst10QuestionsBySimulation(simulationId)
      .stream()
      .sorted(Comparator.comparing(FlowNode::getOrderNumber))
      .toList();

    List<FlowNode> result = new ArrayList<>();

    for (FlowNode question : questions) {
      result.add(question); // Always add the current question

      if (actionableMessageTypes.contains(question.getMessageType().name())) {
        break; // Stop the loop as we've included the first actionable question
      }
    }
    return result;
  }

  public List<FlowNode> findAllBySimulationIdAndPreviousOrderNumber(final Long simulationId, final long previousOrderNumber) {
    return flowRepository.findAllBySimulationIdAndPreviousOrderNumber(simulationId, previousOrderNumber);
  }

}
