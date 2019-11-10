package com.etl;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Opinion {

    private String review;
    private String nickname;
    private int grade;
    private String recommendation;
    private String publishDate;
    private int thumbsUp;
    private int thumbsDown;
    private String advantage;
    private String disadvantage;
}
