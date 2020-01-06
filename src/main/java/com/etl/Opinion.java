package com.etl;

import lombok.*;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Opinion {
    private String id;
    private String review;
    private String nickname;
    private int grade;
    private String recommendation;
    private String publishDate;
    private int thumbsUp;
    private int thumbsDown;
    private String phone_id;
}
