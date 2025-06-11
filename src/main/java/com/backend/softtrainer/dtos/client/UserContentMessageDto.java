package com.backend.softtrainer.dtos.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UserContentMessageDto extends UserMessageDto {

  private List<String> urls;

  private List<String> previews;

  private List<VideoObjDto> videos;

}
