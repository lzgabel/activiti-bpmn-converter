<div align="center">
<img src="backend/static/img/img.jpeg" width="400px"/>

<h1>Activiti BPMN Converter </h1>
<p>
Activiti-BPMN-Converter is a JSON to BPMN converter for activiti.
</p>
</div>


### 1. 节点类型（nodeType)
- serviceTask
    - service 任务节点
- userTask
    - user 任务节点    
- parallelGateway
    - 并行节点
- exclusiveGateway
    - 排他节点

### 2. 数据结构

- serviceTask
```json
{
    "nodeName":"审核人1",
    "nodeType":"serviceTask",
    "nextNode": null
}
```

> 对于 parallel/exclusive 类型，目前建议设置 nextNode 的 nodeType 类型一一对应

- exclusive
```json
{
    "nodeName":"排他",
    "nodeType":"exclusiveGateway",
    "nextNode":{
        "nodeName":"",
        "nodeType":"exclusiveGateway",
        "nextNode":null
    },
    "branchNodes":[
        {
            "nodeName":"条件1",
            "conditionExpression":"=id>1",
            "nextNode":{
                "nodeName":"审核人2.1",
                "nodeType":"userTask",
                "nextNode": null
            }
        },
        {
            "nodeName":"条件2",
            "conditionExpression":"=id<1",
            "nextNode":{
                "nodeName":"审核人2.2",
                "nodeType":"userTask",
                "nextNode":null
            }
        },
        {...}
    ]
}
```

- parallel
```json
{
    "nodeName":"并行任务",
    "nodeType":"parallelGateway",
    "nextNode":{
        "nodeName":"",
        "nodeType":"parallelGateway",
        "nextNode":null
    },
    "branchNodes":[
        {
            "nextNode":{
                "nodeName":"审核人2.1",
                "nodeType":"userTask",
                "nextNode": null
            }
        },
        {
            "nextNode":{
                "nodeName":"审核人2.2",
                "nodeType":"userTask",
                "nextNode":null
            }
        },
        {
            "nextNode":{
                "nodeName":"审核人2.3",
                "nodeType":"userTask",
                "nextNode":null
            }
        },
        {...}
    ]
}
```

- 完整数据结构
```json
{
    "process":{
        "processId":"work-flow-id",
        "name":"合同审批"
    },
    "processNode":{
        "nodeName":"审核人1",
        "nodeType":"userTask",
        "candidateGroups": "计算中心,行政部",
        "taskHeaders":{
            "a":"b",
            "e":"d"
        },
        "nextNode":{
            "nodeName":"排他",
            "nodeType":"exclusiveGateway",
            "nextNode":{
                "nodeName":"排他网关",
                "nodeType":"exclusiveGateway",
                "nextNode":null
            },
            "branchNodes":[
                {
                    "nodeName":"条件1",
                    "conditionExpression":"=id>1",
                    "nextNode":{
                        "nodeName":"审核人2.1",
                        "nodeType":"userTask",
                        "assignee": "张三",
                        "candidateUsers": "张三,李四",
                        "candidateGroups": "计算中心,行政部",
                        "nextNode":null
                    }
                },
                {
                    "nodeName":"条件2",
                    "conditionExpression":"=id<1",
                    "nextNode":{
                        "nodeName":"审核人2.2",
                        "nodeType":"userTask",
                        "assignee": "张三",
                        "nextNode":null
                    }
                }
            ]
        }
    }
}
```
### 3. 示例
* 启动服务
    * ``mvn spring-boot:run``
    * http://localhost:8080
