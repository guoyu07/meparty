package com.teammental.mevalidation.dto;

import com.teammental.mecore.stereotype.dto.Dto;
import java.util.ArrayList;
import java.util.List;

public class ValidationResultDto implements Dto {

  private List<FieldErrorDto> fieldErrors;
  private List<GlobalErrorDto> globalErrors;

  public List<FieldErrorDto> getFieldErrors() {
    return fieldErrors;
  }

  public ValidationResultDto() {
    fieldErrors = new ArrayList<>();
    globalErrors = new ArrayList<>();
  }

  /**
   * Set field errors.
   * @param fieldErrors field errors
   */
  public void setFieldErrors(List<FieldErrorDto> fieldErrors) {
    if (fieldErrors != null) {
      this.fieldErrors = fieldErrors;
    }
  }

  /**
   * Add a new field error.
   * @param fieldName name for the fieldError
   * @param message message for the fieldError
   */
  public void addFieldError(String fieldName, String message) {
    FieldErrorDto fieldErrorDto = new FieldErrorDto(fieldName, message);
    fieldErrors.add(fieldErrorDto);
  }

  /**
   * Add a new field error.
   * @param fieldErrorDto fieldError object
   */
  public void addFieldError(FieldErrorDto fieldErrorDto) {
    if (fieldErrorDto != null) {
      fieldErrors.add(fieldErrorDto);
    }
  }

  public List<GlobalErrorDto> getGlobalErrors() {
    return globalErrors;
  }

  /**
   * Set global errors.
   * @param globalErrors globalErrors
   */
  public void setGlobalErrors(List<GlobalErrorDto> globalErrors) {
    if (globalErrors != null) {
      this.globalErrors = globalErrors;
    }
  }

  /**
   * Add a new global error.
   * @param message message for the global error
   */
  public void addGlobalError(String message) {
    GlobalErrorDto globalErrorDto = new GlobalErrorDto();
    globalErrorDto.setMessage(message);
    globalErrors.add(globalErrorDto);
  }

  /**
   * Add a new global error.
   * @param globalErrorDto global error object
   */
  public void addGlobalError(GlobalErrorDto globalErrorDto) {
    if (globalErrorDto != null) {
      globalErrors.add(globalErrorDto);
    }
  }

  /**
   * Checks if there are any field errors.
   * @return true if any field error exists.
   */
  public boolean hasFieldError() {
    return fieldErrors != null
        && !fieldErrors.isEmpty();
  }

  /**
   * Checks if there are any global errors.
   * @return true if any global error exists.
   */
  public boolean hasGlobalError() {
    return globalErrors != null
        && !globalErrors.isEmpty();
  }
}
