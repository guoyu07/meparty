package com.teammental.mevalidation.constraint.to.multipartfileimage;

import com.teammental.mevalidation.constraint.MultipartFileImageConstraint;
import org.springframework.web.multipart.MultipartFile;

public class ImageMinHeight500To {

  @MultipartFileImageConstraint(minHeight = 500)
  private MultipartFile multipartFile;

  public MultipartFile getMultipartFile() {
    return multipartFile;
  }

  public void setMultipartFile(MultipartFile multipartFile) {
    this.multipartFile = multipartFile;
  }
}
