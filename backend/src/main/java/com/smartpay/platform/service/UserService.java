package com.smartpay.platform.service;

import com.smartpay.platform.dto.PageResponse;
import com.smartpay.platform.dto.UserResponse;
import com.smartpay.platform.exception.ResourceNotFoundException;
import com.smartpay.platform.mapper.UserMapper;
import com.smartpay.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id).map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String search, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search == null || search.isBlank()) {
            return PageResponse.from(userRepository.findAll(pageable).map(UserMapper::toResponse));
        }
        String like = "%" + search.trim().toLowerCase() + "%";
        return PageResponse.from(userRepository.search(like, pageable).map(UserMapper::toResponse));
    }
}
