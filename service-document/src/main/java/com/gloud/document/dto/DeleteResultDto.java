package com.gloud.document.dto;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteResultDto {
    private List<Long> successIds;
    private List<FailedDeleteInfo> failed;
}