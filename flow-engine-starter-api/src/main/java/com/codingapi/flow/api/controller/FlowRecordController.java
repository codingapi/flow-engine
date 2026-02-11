package com.codingapi.flow.api.controller;

import com.codingapi.springboot.framework.dto.request.IdRequest;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cmd/record")
@AllArgsConstructor
public class FlowRecordController {

    @GetMapping("/detail")
    public Response detail(IdRequest request) {
        String id = request.getStringId();
        System.out.println("id:" + id);
        return SingleResponse.empty();
    }

}
