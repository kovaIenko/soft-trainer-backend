package com.backend.softtrainer.dtos.flow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentQuestionDto extends FlowQuestionDto {

  private String url;
//
//  public List<FlowRecord> getFlowRecords() {
//    return getPreviousOrderNumber().stream()
//      .map(prevMessageId ->
//             ContentQuestion.builder()
//               .orderNumber(getMessageId())
//               .showPredicate(getShowPredicate())
//               .url(url)
//               .previousOrderNumber(prevMessageId)
//               .name(getName())
//               .messageType(com.backend.softtrainer.interpreter.MessageType.CONTENT_QUESTION)
//               .build())
//      .collect(Collectors.toList());
//  }

}
