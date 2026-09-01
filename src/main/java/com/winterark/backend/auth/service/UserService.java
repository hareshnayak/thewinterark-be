package com.winterark.backend.auth.service;

import com.winterark.backend.auth.domain.User;
import com.winterark.backend.auth.domain.UserRepository;
import com.winterark.backend.auth.payload.UserSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserSearchResponseDTO> searchUsers(User currentUser, String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<User> matchedUsers = userRepository.searchUsersNotConnected(query.trim(), currentUser.getId());

        return matchedUsers.stream()
                .map(u -> UserSearchResponseDTO.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
