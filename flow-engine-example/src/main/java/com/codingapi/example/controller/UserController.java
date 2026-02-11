package com.codingapi.example.controller;

import com.codingapi.example.entity.User;
import com.codingapi.example.repository.UserRepository;
import com.codingapi.example.service.UserService;
import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.request.SearchRequest;
import com.codingapi.springboot.framework.dto.response.MultiResponse;
import com.codingapi.springboot.framework.dto.response.Response;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    private final UserRepository userRepository;

    @GetMapping("/list")
    public MultiResponse<User> list(SearchRequest request){
        return MultiResponse.of(userRepository.searchRequest(request));
    }

    @PostMapping("/save")
    public Response save(@RequestBody User request){
        userService.save(request);
        return Response.buildSuccess();
    }

    @PostMapping("/remove")
    public Response remove(@RequestBody IdRequest request){
        userService.remove(request.getLongId());
        return Response.buildSuccess();
    }

}
