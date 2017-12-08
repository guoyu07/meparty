package com.teammental.medto;

import com.teammental.core.dto.Dto;

import java.io.Serializable;

public interface IdDto<IdT extends Serializable> extends Dto {
  IdT getId();

  void setId(IdT id);
}
