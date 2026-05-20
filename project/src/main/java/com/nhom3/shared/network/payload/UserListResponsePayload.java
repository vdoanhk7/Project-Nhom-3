package com.nhom3.shared.network.payload;

import java.util.List;

public class UserListResponsePayload {
    private final List<UserDTO> users;

    public UserListResponsePayload(List<UserDTO> users) {
        this.users = users;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public static class UserDTO {
        public final int id;
        public final String username;
        public final String fullName;
        public final String role;
        public final String email;
        public final String phone;
        public final int reputationScore;

        public UserDTO(int id, String username, String fullName,
                String role, String email, String phone) {
            this(id, username, fullName, role, email, phone,
                    com.nhom3.shared.model.user.User.DEFAULT_REPUTATION_SCORE);
        }

        public UserDTO(int id, String username, String fullName,
                String role, String email, String phone, int reputationScore) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.email = email;
            this.phone = phone;
            this.reputationScore = reputationScore;
        }
    }
}
