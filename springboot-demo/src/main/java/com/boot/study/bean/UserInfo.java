package com.boot.study.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long id;

    private String username;

    private String token;

    /**
     * 管理端用户 ADMIN
     * 教师端用户 TEACHER
     * 学生端    STUDENT
     */
    private String role;
}
