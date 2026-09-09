package com.enterprise.courseservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAssignmentRequest {

    private String textAnswer;

    private String fileUrls;
}
