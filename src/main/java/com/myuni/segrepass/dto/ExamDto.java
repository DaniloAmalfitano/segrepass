package com.myuni.segrepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDto {
    private String courseCode;
    private String courseName;
    private String cfu;
    private String grade;
    private String date;
}
