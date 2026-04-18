package com.myuni.segrepass.dto;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class SummaryDto {
    private String cfuMatured;
    private String cfuMissing;
    private String cfuTotal;
    private String examsTaken;
    private String averageExams;
    private String weightedAverage;
    private String arithmeticAverage;
    private String weightedAverageOn110;
    private String arithmeticAverageOn110;
    private String numberOfHonors;
}
