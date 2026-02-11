package com.example.sejong_voice.post.vote.entity;

import com.example.sejong_voice.exception.BusinessException;
import com.example.sejong_voice.post.vote.exception.VoteErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VoteChoiceConverter implements AttributeConverter<VoteChoice, Integer> {

  @Override
  public Integer convertToDatabaseColumn(VoteChoice attribute) {
    if (attribute == null) return null;
    return attribute.getValue();
  }

  @Override
  public VoteChoice convertToEntityAttribute(Integer dbData) {
    if (dbData == null) return null;

    for (VoteChoice choice : VoteChoice.values()) {
      if (choice.getValue() == dbData) {
        return choice;
      }
    }
    throw new BusinessException(VoteErrorCode.UNKNOWN_VOTE_VALUE);
  }
}