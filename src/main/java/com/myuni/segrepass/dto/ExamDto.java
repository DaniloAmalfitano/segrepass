package com.myuni.segrepass.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExamDto {
    private String courseCode;
    private String courseName;
    private String cfu;
    private String grade;
    private String date;
}
