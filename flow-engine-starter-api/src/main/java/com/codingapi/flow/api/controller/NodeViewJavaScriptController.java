package com.codingapi.flow.api.controller;

import com.codingapi.flow.api.pojo.NodeViewJavaScriptRequest;
import com.codingapi.flow.javscript.NodeViewJavaScript;
import com.codingapi.flow.javscript.NodeViewJavaScriptCacheContext;
import com.codingapi.springboot.framework.dto.response.Response;
import com.codingapi.springboot.framework.dto.response.SingleResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cmd/node-view")
public class NodeViewJavaScriptController {

    @GetMapping("/getScript")
    public SingleResponse<String> getScript(@RequestParam(name = "key") String key) {
        NodeViewJavaScript nodeViewJavaScript = NodeViewJavaScriptCacheContext.getInstance().get(key);
        if (nodeViewJavaScript != null) {
            return SingleResponse.of(nodeViewJavaScript.getScript());
        }
        return SingleResponse.of("");
    }

    @PostMapping("/save")
    public Response save(@RequestBody NodeViewJavaScriptRequest request) {
        NodeViewJavaScriptCacheContext.getInstance().cache(request.getCode(), request.getScript());
        return Response.buildSuccess();
    }

}
