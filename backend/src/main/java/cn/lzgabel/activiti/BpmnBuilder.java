
package cn.lzgabel.activiti;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 〈功能简述〉<br>
 * 〈基于 json 格式自动生成 bpmn 文件〉
 *
 * @author lizhi
 * @date 2021-09-07
 * @since 1.0.0
 */

public class BpmnBuilder {

    private static BpmnModel model;
    private static Process process;
    private static List<SequenceFlow> sequenceFlows;

    public static void main(String[] args) {
        String json = "{\n" +
                "    \"process\":{\n" +
                "        \"processId\":\"work-flow-id\",\n" +
                "        \"name\":\"自动生成\"\n" +
                "    },\n" +
                "    \"processNode\":{\n" +
                "        \"nodeName\":\"审核人\",\n" +
                "        \"id\":\"user-78a98767-e311-4757-91fb-ccf1676a3b2b\",\n" +
                "        \"jobType\":\"user-78a98767-e311-4757-91fb-ccf1676a3b2b\",\n" +
                "        \"type\":\"task\",\n" +
                "        \"nodeType\":\"serviceTask\",\n" +
                "        \"nextNode\":{\n" +
                "            \"nodeName\":\"审核人\",\n" +
                "            \"id\":\"user-8f75a8f7-2e0d-4b56-b0de-dd9848dfe9f0\",\n" +
                "            \"jobType\":\"user-8f75a8f7-2e0d-4b56-b0de-dd9848dfe9f0\",\n" +
                "            \"type\":\"task\",\n" +
                "            \"nodeType\":\"serviceTask\",\n" +
                "            \"nextNode\":null,\n" +
                "            \"error\":true\n" +
                "        }\n" +
                "    }\n" +
                "}";

        String s = build(json);
        System.out.println(s);
    }


    public static String build(String json) {

        try {
            model = new BpmnModel();
            process = new Process();
            sequenceFlows = Lists.newArrayList();
            JSONObject object = JSON.parseObject(json, JSONObject.class);
            JSONObject workflow = object.getJSONObject("process");
            model.addProcess(process);
            process.setName(workflow.getString("name"));
            process.setId(workflow.getString("processId"));
            StartEvent startEvent = createStartEvent();
            process.addFlowElement(startEvent);
            JSONObject flowNode = object.getJSONObject("processNode");
            String lastNode = create(startEvent.getId(), flowNode);
            EndEvent endEvent = createEndEvent();
            process.addFlowElement(endEvent);
            process.addFlowElement(connect(lastNode, endEvent.getId()));

            new BpmnAutoLayout(model).execute();
            return new String(new BpmnXMLConverter().convertToXML(model));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建失败: e=" + e.getMessage());
        }
    }


    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    private static ServiceTask serviceTask(String name) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setName(name);
        return serviceTask;
    }

    protected static SequenceFlow connect(String from, String to) {
        SequenceFlow flow = new SequenceFlow();
        flow.setId(id("sequenceFlow"));
        flow.setSourceRef(from);
        flow.setTargetRef(to);
        sequenceFlows.add(flow);
        return flow;
    }

    protected static StartEvent createStartEvent() {
        StartEvent startEvent = new StartEvent();
        startEvent.setId(id("start"));
        return startEvent;
    }

    protected static EndEvent createEndEvent() {
        EndEvent endEvent = new EndEvent();
        endEvent.setId(id("end"));
        return endEvent;
    }


    private static String create(String fromId, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
        String nodeType = flowNode.getString("nodeType");
        if (Type.PARALLEL_GATEWAY.isEqual(nodeType)) {
            return createParallelGatewayBuilder(fromId, flowNode);
        } else if (Type.EXCLUSIVE_GATEWAY.isEqual(nodeType)) {
            return createExclusiveGatewayBuilder(fromId, flowNode);
        } else if (Type.SERVICE_TASK.isEqual(nodeType)) {
            flowNode.put("incoming", Collections.singletonList(fromId));
            String id = createServiceTask(flowNode);

            // 如果当前任务还有后续任务，则遍历创建后续任务
            JSONObject nextNode = flowNode.getJSONObject("nextNode");
            if (Objects.nonNull(nextNode)) {
                FlowElement flowElement = model.getFlowElement(id);
                return create(id, nextNode);
            } else {
                return id;
            }
        } else if (Type.USER_TASK.isEqual(nodeType)) {
            flowNode.put("incoming", Collections.singletonList(fromId));
            String id = createUserTask(flowNode);

            // 如果当前任务还有后续任务，则遍历创建后续任务
            JSONObject nextNode = flowNode.getJSONObject("nextNode");
            if (Objects.nonNull(nextNode)) {
                FlowElement flowElement = model.getFlowElement(id);
                return create(id, nextNode);
            } else {
                return id;
            }
        } else {
            throw new RuntimeException("未知节点类型: nodeType=" + nodeType);
        }
    }

    private static String createExclusiveGatewayBuilder(String formId, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
        String name = flowNode.getString("nodeName");
        String exclusiveGatewayId = id("exclusiveGateway");
        ExclusiveGateway exclusiveGateway = new ExclusiveGateway();
        exclusiveGateway.setId(exclusiveGatewayId);
        exclusiveGateway.setName(name);
        process.addFlowElement(exclusiveGateway);
        process.addFlowElement(connect(formId, exclusiveGatewayId));

        if (Objects.isNull(flowNode.getJSONArray("branchNodes")) && Objects.isNull(flowNode.getJSONObject("nextNode"))) {
            return exclusiveGatewayId;
        }
        List<JSONObject> flowNodes = Optional.ofNullable(flowNode.getJSONArray("branchNodes")).map(e -> e.toJavaList(JSONObject.class)).orElse(Collections.emptyList());
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());

        List<JSONObject> conditions = Lists.newCopyOnWriteArrayList();
        for (JSONObject element : flowNodes) {
            JSONObject childNode = element.getJSONObject("nextNode");

            String nodeName = element.getString("nodeName");
            String expression = element.getString("conditionExpression");

            if (Objects.isNull(childNode)) {
                incoming.add(exclusiveGatewayId);
                JSONObject condition = new JSONObject();
                condition.fluentPut("nodeName", nodeName)
                        .fluentPut("expression", expression);
                conditions.add(condition);
                continue;
            }
            // 只生成一个任务，同时设置当前任务的条件
            childNode.put("incoming", Collections.singletonList(exclusiveGatewayId));
            String identifier = create(exclusiveGatewayId, childNode);
            List<SequenceFlow> flows = sequenceFlows.stream().filter(flow -> StringUtils.equals(exclusiveGatewayId, flow.getSourceRef()))
                    .collect(Collectors.toList());
            flows.stream().forEach(
                    e -> {
                        if (StringUtils.isBlank(e.getName()) && StringUtils.isNotBlank(nodeName)) {
                            e.setName(nodeName);
                        }
                        // 设置条件表达式
                        if (Objects.isNull(e.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                            e.setConditionExpression(expression);
                        }
                    }
            );
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }


        JSONObject childNode = flowNode.getJSONObject("nextNode");
        if (Objects.nonNull(childNode)) {
            if (incoming == null || incoming.isEmpty()) {
                return create(exclusiveGatewayId, childNode);
            } else {
                // 所有 service task 连接 end exclusive gateway
                childNode.put("incoming", incoming);
                FlowElement flowElement = model.getFlowElement(incoming.get(0));
                // 1.0 先进行边连接, 暂存 nextNode
                JSONObject nextNode = childNode.getJSONObject("nextNode");
                childNode.put("nextNode", null);
                String identifier = create(flowElement.getId(), childNode);
                for (int i = 1; i < incoming.size(); i++) {
                    process.addFlowElement(connect(incoming.get(i), identifier));
                }

                //  针对 gateway 空任务分支 添加条件表达式
                if (!conditions.isEmpty()) {
                    FlowElement flowElement1 = model.getFlowElement(identifier);
                    // 获取从 gateway 到目标节点 未设置条件表达式的节点
                    List<SequenceFlow> flows = sequenceFlows.stream().filter(flow -> StringUtils.equals(flowElement1.getId(), flow.getTargetRef()))
                            .filter(flow -> StringUtils.equals(flow.getSourceRef(), exclusiveGatewayId))
                            .collect(Collectors.toList());
                    flows.stream().forEach(sequenceFlow -> {
                        if (!conditions.isEmpty()) {
                            JSONObject condition = conditions.get(0);
                            String nodeName = condition.getString("nodeName");
                            String expression = condition.getString("expression");

                            if (StringUtils.isBlank(sequenceFlow.getName()) && StringUtils.isNotBlank(nodeName)) {
                                sequenceFlow.setName(nodeName);
                            }
                            // 设置条件表达式
                            if (Objects.isNull(sequenceFlow.getConditionExpression()) && StringUtils.isNotBlank(expression)) {
                                sequenceFlow.setConditionExpression(expression);
                            }

                            conditions.remove(0);
                        }
                    });

                }

                // 1.1 边连接完成后，在进行 nextNode 创建
                if (Objects.nonNull(nextNode)) {
                    return create(identifier, nextNode);
                } else {
                    return identifier;
                }
            }
        }
        return exclusiveGatewayId;
    }

    private static String createParallelGatewayBuilder(String formId, JSONObject flowNode) throws InvocationTargetException, IllegalAccessException {
        String name = flowNode.getString("nodeName");
        ParallelGateway parallelGateway = new ParallelGateway();
        String parallelGatewayId = id("parallelGateway");
        parallelGateway.setId(parallelGatewayId);
        parallelGateway.setName(name);
        process.addFlowElement(parallelGateway);
        process.addFlowElement(connect(formId, parallelGatewayId));

        if (Objects.isNull(flowNode.getJSONArray("branchNodes"))
                && Objects.isNull(flowNode.getJSONObject("nextNode"))) {
            return parallelGatewayId;
        }

        List<JSONObject> flowNodes = Optional.ofNullable(flowNode.getJSONArray("branchNodes")).map(e -> e.toJavaList(JSONObject.class)).orElse(Collections.emptyList());
        List<String> incoming = Lists.newArrayListWithCapacity(flowNodes.size());
        for (JSONObject element : flowNodes) {
            JSONObject childNode = element.getJSONObject("nextNode");
            if (Objects.isNull(childNode)) {
                incoming.add(parallelGatewayId);
                continue;
            }
            String identifier = create(parallelGatewayId, childNode);
            if (Objects.nonNull(identifier)) {
                incoming.add(identifier);
            }
        }

        JSONObject childNode = flowNode.getJSONObject("nextNode");
        if (Objects.nonNull(childNode)) {
            // 普通结束网关
            if (CollectionUtils.isEmpty(incoming)) {
                return create(parallelGatewayId, childNode);
            } else {
                // 所有 service task 连接 end parallel gateway
                childNode.put("incoming", incoming);
                FlowElement flowElement = model.getFlowElement(incoming.get(0));
                // 1.0 先进行边连接, 暂存 nextNode
                JSONObject nextNode = childNode.getJSONObject("nextNode");
                childNode.put("nextNode", null);
                String identifier = create(incoming.get(0), childNode);
                for (int i = 1; i < incoming.size(); i++) {
                    FlowElement flowElement1 = model.getFlowElement(incoming.get(i));
                    process.addFlowElement(connect(flowElement1.getId(), identifier));
                }
                // 1.1 边连接完成后，在进行 nextNode 创建
                if (Objects.nonNull(nextNode)) {
                    return create(identifier, nextNode);
                } else {
                    return identifier;
                }
            }
        }
        return parallelGatewayId;
    }

    private static String createServiceTask(JSONObject flowNode) {
        List<String> incoming = flowNode.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
        String id = id("serviceTask");
        if (incoming != null && !incoming.isEmpty()) {
            ServiceTask serviceTask = new ServiceTask();
            serviceTask.setName(flowNode.getString("nodeName"));
            serviceTask.setId(id);
            process.addFlowElement(serviceTask);
            process.addFlowElement(connect(incoming.get(0), id));
        }
        return id;
    }

    private static String createUserTask(JSONObject flowNode) {
        List<String> incoming = flowNode.getJSONArray("incoming").toJavaList(String.class);
        // 自动生成id
        String id = id("userTask");
        if (incoming != null && !incoming.isEmpty()) {
            UserTask userTask = new UserTask();
            userTask.setName(flowNode.getString("nodeName"));
            userTask.setId(id);
            Optional.ofNullable(flowNode.getString("assignee"))
                    .filter(StringUtils::isNotBlank)
                    .ifPresent(userTask::setAssignee);

            Optional.ofNullable(flowNode.getJSONArray("candidateUsers"))
                    .map(e -> e.toJavaList(String.class))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(userTask::setCandidateUsers);


            Optional.ofNullable(flowNode.getJSONArray("candidateGroups"))
                    .map(e -> e.toJavaList(String.class))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(userTask::setCandidateGroups);

            process.addFlowElement(userTask);
            process.addFlowElement(connect(incoming.get(0), id));
        }
        return id;
    }

    /**
     * 循环向上转型, 获取对象的 DeclaredMethod
     *
     * @param object         : 子类对象
     * @param methodName     : 父类中的方法名
     * @param parameterTypes : 父类中的方法参数类型
     * @return 父类中的方法对象
     */
    private static Method getDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                return method;
            } catch (Exception ignore) {
            }
        }
        return null;
    }


    private enum Type {

        /**
         * 并行事件
         */
        PARALLEL_GATEWAY("parallelGateway", ParallelGateway.class),

        /**
         * 排他事件
         */
        EXCLUSIVE_GATEWAY("exclusiveGateway", ExclusiveGateway.class),

        /**
         * 用户任务
         */
        USER_TASK("userTask", UserTask.class),

        /**
         * 任务
         */
        SERVICE_TASK("serviceTask", ServiceTask.class);

        private String type;

        private Class<?> typeClass;

        Type(String type, Class<?> typeClass) {
            this.type = type;
            this.typeClass = typeClass;
        }

        public final static Map<String, Class<?>> TYPE_MAP = Maps.newHashMap();

        static {
            for (Type element : Type.values()) {
                TYPE_MAP.put(element.type, element.typeClass);
            }
        }

        public boolean isEqual(String type) {
            return this.type.equals(type);
        }

    }
}
