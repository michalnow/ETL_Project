package com.etl;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Phone {
    private String phone_id;
    private String fullName;
    private String description;
    private String imageUrl;
}
