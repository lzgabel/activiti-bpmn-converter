package cn.lzgabel.activiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 〈功能简述〉<br>
 * 〈〉
 *
 * @author lizhi
 * @date 2021-09-07
 * @since 1.0.0
 */

@SpringBootApplication
@ComponentScan(basePackages = "cn.lzgabel")
public class ActivitiBpmnGenerateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitiBpmnGenerateApplication.class, args);
    }

}
