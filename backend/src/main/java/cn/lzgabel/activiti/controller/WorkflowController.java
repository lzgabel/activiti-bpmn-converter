package cn.lzgabel.activiti.controller;


import cn.lzgabel.activiti.BpmnBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021-09-07
 * @since 1.0.0
 */

@RestController
@RequestMapping
public class WorkflowController {

    @GetMapping("/ok")
    public String ok() {
        return "ok";
    }

    @PostMapping(value = "/deploy",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @CrossOrigin
    public byte[] deploy(@RequestBody DeployRequest request) {
        String xml = BpmnBuilder.build(request.getJson());
        return xml.getBytes();
    }

}
