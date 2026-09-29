package com.backend.orbitflow.domain.user.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.orbitflow.domain.user.dto.response.UserSuccessCode;
import com.backend.orbitflow.domain.user.facade.UserFacade;
import com.backend.orbitflow.global.common.dto.response.CommonResponse;
import com.backend.orbitflow.global.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserFacade userFacade;

    @PostMapping
    public ResponseEntity<CommonResponse<UserResponse>> signUp (
        @Valid @RequestBody UserSignupRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(CommonResponse.success(
                UserSuccessCode.USER_SIGNUP_SUCCESS,
                userFacade.signup(request)
            ));
    }
    
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<UserResponse>> getOwnUser(
        @AuthenticationPrincipal AuthUser authUser
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(CommonResponse.success(
                UserSuccessCode.GET_USER_INFO,
                userFacade.getOwnUser(authUser)
            ));
    }

    @PutMapping("me/updateProfile")
    public String putMethodName(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }

    @PutMapping("/me/email")
    public String putMethodName(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }
    
    @PutMapping("path/{id}")
    public String putMethodName(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }

    @PutMapping("path/{id}")
    public String putMethodName(@PathVariable String id, @RequestBody String entity) {
        //TODO: process PUT request
        
        return entity;
    }

    @DeleteMapping("/me")
    public String deleteMethodName() {
        
    }
}
