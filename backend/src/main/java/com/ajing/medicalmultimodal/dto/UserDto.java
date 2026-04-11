package com.ajing.medicalmultimodal.dto;

import com.ajing.medicalmultimodal.domain.AppUser;

public record UserDto(String id, String name, String age, String gender, String phone) {
    public static UserDto from(AppUser u) {
        return new UserDto(
                String.valueOf(u.getId()),
                u.getName(),
                u.getAge() != null ? u.getAge() : "",
                u.getGender() != null ? u.getGender() : "",
                u.getPhone() != null ? u.getPhone() : "");
    }
}
