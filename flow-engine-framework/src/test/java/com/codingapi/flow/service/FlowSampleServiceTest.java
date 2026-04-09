package com.codingapi.flow.service;

import com.codingapi.flow.action.IFlowAction;
import com.codingapi.flow.action.actions.CustomAction;
import com.codingapi.flow.builder.ActionBuilder;
import com.codingapi.flow.builder.FormFieldPermissionsBuilder;
import com.codingapi.flow.builder.NodeStrategyBuilder;
import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.factory.MyFlowServiceFactory;
import com.codingapi.flow.form.DataType;
import com.codingapi.flow.form.FlowForm;
import com.codingapi.flow.form.FlowFormBuilder;
import com.codingapi.flow.form.permission.PermissionType;
import com.codingapi.flow.node.nodes.*;
import com.codingapi.flow.pojo.body.FlowAdviceBody;
import com.codingapi.flow.pojo.request.FlowActionRequest;
import com.codingapi.flow.pojo.request.FlowCreateRequest;
import com.codingapi.flow.pojo.request.FlowRevokeRequest;
import com.codingapi.flow.pojo.request.FlowUrgeRequest;
import com.codingapi.flow.pojo.response.ActionResponse;
import com.codingapi.flow.record.FlowRecord;
import com.codingapi.flow.script.runtime.FlowScriptContext;
import com.codingapi.flow.script.runtime.IBeanFactory;
import com.codingapi.flow.strategy.node.ErrorTriggerStrategy;
import com.codingapi.flow.strategy.node.FormFieldPermissionStrategy;
import com.codingapi.flow.strategy.node.OperatorLoadStrategy;
import com.codingapi.flow.strategy.node.RouterStrategy;
import com.codingapi.flow.user.User;
import com.codingapi.flow.workflow.Workflow;
import com.codingapi.flow.workflow.WorkflowBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlowSampleServiceTest {

    private final MyFlowServiceFactory factory = new MyFlowServiceFactory();

    @Test
    void create() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());
    }


    @Test
    void importWorkflow() {
        User user = new User(1, "user");
        String data = "data:application/json;base64,eyJ1cGRhdGVkVGltZSI6IjE3NzM0MTY3MjI3NzMiLCJjb2RlIjoiMFVseXZtM3RaYiIsIm5vZGVzIjpbeyJ2aWV3IjoiZGVmYXVsdCIsInN0cmF0ZWdpZXMiOlt7InNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5L2g5pyJ5LiA5p2h5b6F5YqeXG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiAn5L2g5pyJ5LiA5p2h5b6F5YqeJ1xufVxuIiwic3RyYXRlZ3lUeXBlIjoiTm9kZVRpdGxlU3RyYXRlZ3kifSx7InN0cmF0ZWd5VHlwZSI6IkZvcm1GaWVsZFBlcm1pc3Npb25TdHJhdGVneSIsImZpZWxkUGVybWlzc2lvbnMiOltdfSx7ImVuYWJsZSI6dHJ1ZSwidHlwZSI6IlJFVk9LRV9DVVJSRU5UIiwic3RyYXRlZ3lUeXBlIjoiUmV2b2tlU3RyYXRlZ3kifV0sImRpc3BsYXkiOnRydWUsIm5hbWUiOiLlvIDlp4voioLngrkiLCJpZCI6IjAwYUFPdTYyN0FuM3ZLZ1hYayIsInR5cGUiOiJTVEFSVCIsImFjdGlvbnMiOlt7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IumAmui/hyJ9LCJpZCI6IkZWT05CaXNzYVhtaTdLYldNSyIsInR5cGUiOiJQQVNTIiwidGl0bGUiOiLpgJrov4cifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IuS/neWtmCJ9LCJpZCI6ImVWNTRsRGtUbzhJdGtGRkZaUCIsInR5cGUiOiJTQVZFIiwidGl0bGUiOiLkv53lrZgifV0sIm9yZGVyIjoiMCJ9LHsic3RyYXRlZ2llcyI6eyIkcmVmIjoiJC5ub2Rlc1swXS5zdHJhdGVnaWVzWzFdLmZpZWxkUGVybWlzc2lvbnMifSwiYmxvY2tzIjpbeyJzdHJhdGVnaWVzIjp7IiRyZWYiOiIkLm5vZGVzWzBdLnN0cmF0ZWdpZXNbMV0uZmllbGRQZXJtaXNzaW9ucyJ9LCJibG9ja3MiOlt7InZpZXciOiJkZWZhdWx0Iiwic3RyYXRlZ2llcyI6W3sidGltZW91dFRpbWUiOiI4NjQwMDAwMCIsInR5cGUiOiJSRU1JTkQiLCJzdHJhdGVneVR5cGUiOiJUaW1lb3V0U3RyYXRlZ3kifSx7InR5cGUiOiJTRVFVRU5DRSIsInBlcmNlbnQiOiIwLjAiLCJzdHJhdGVneVR5cGUiOiJNdWx0aU9wZXJhdG9yQXVkaXRTdHJhdGVneSJ9LHsidHlwZSI6IkFVVE9fUEFTUyIsInN0cmF0ZWd5VHlwZSI6IlNhbWVPcGVyYXRvckF1ZGl0U3RyYXRlZ3kifSx7ImVuYWJsZSI6ZmFsc2UsInN0cmF0ZWd5VHlwZSI6IlJlY29yZE1lcmdlU3RyYXRlZ3kifSx7InR5cGUiOiJSRVNVTUUiLCJzdHJhdGVneVR5cGUiOiJSZXN1Ym1pdFN0cmF0ZWd5In0seyJzaWduUmVxdWlyZWQiOmZhbHNlLCJhZHZpY2VSZXF1aXJlZCI6ZmFsc2UsInN0cmF0ZWd5VHlwZSI6IkFkdmljZVN0cmF0ZWd5In0seyJzY3JpcHQiOiIvLyBAU0NSSVBUX1RJVExFIOWbnumAgOiHs+W8gOWni+iKgueCuVxuLy8gQFNDUklQVF9NRVRBIHtcInR5cGVcIjpcIm5vZGVcIixcIm5vZGVcIjpcIlNUQVJUXCJ9XG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiByZXF1ZXN0LmdldFN0YXJ0Tm9kZSgpLmdldElkKCk7XG59XG4iLCJzdHJhdGVneVR5cGUiOiJFcnJvclRyaWdnZXJTdHJhdGVneSJ9LHsic2NyaXB0IjoiLy8gQFNDUklQVF9USVRMRSDkvaDmnInkuIDmnaHlvoXlip5cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuICfkvaDmnInkuIDmnaHlvoXlip4nXG59XG4iLCJzdHJhdGVneVR5cGUiOiJOb2RlVGl0bGVTdHJhdGVneSJ9LHsic3RyYXRlZ3lUeXBlIjoiRm9ybUZpZWxkUGVybWlzc2lvblN0cmF0ZWd5IiwiZmllbGRQZXJtaXNzaW9ucyI6eyIkcmVmIjoiJC5ub2Rlc1swXS5zdHJhdGVnaWVzWzFdLmZpZWxkUGVybWlzc2lvbnMifX0seyJzY3JpcHQiOiIvLyBAU0NSSVBUX1RJVExFIOa1geeoi+WIm+W7uuiAhVxuLy8gQFNDUklQVF9NRVRBIHtcInR5cGVcIjpcImNyZWF0b3JcIn1cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuIFtyZXF1ZXN0LmdldENyZWF0ZWRPcGVyYXRvcklkKCldXG59XG4iLCJzdHJhdGVneVR5cGUiOiJPcGVyYXRvckxvYWRTdHJhdGVneSJ9LHsiZW5hYmxlIjp0cnVlLCJ0eXBlIjoiUkVWT0tFX0NVUlJFTlQiLCJzdHJhdGVneVR5cGUiOiJSZXZva2VTdHJhdGVneSJ9XSwiZGlzcGxheSI6dHJ1ZSwibmFtZSI6IuiAgeadv+iKgueCuSIsImlkIjoib1NpRGc2b2pXT3FRUjVRYXp2IiwidHlwZSI6IkFQUFJPVkFMIiwiYWN0aW9ucyI6W3siZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi6YCa6L+HIn0sImlkIjoick1rcnEwV29pUmE4M285MjJvIiwidHlwZSI6IlBBU1MiLCJ0aXRsZSI6IumAmui/hyJ9LHsiZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi5ouS57udIn0sImlkIjoiUzdiZkFUUXJKYmg5MzRnQXo1IiwidHlwZSI6IlJFSkVDVCIsInRpdGxlIjoi5ouS57udIiwic2NyaXB0IjoiLy8gQFNDUklQVF9USVRMRSDov5Tlm57lvIDlp4voioLngrlcbi8vIEBTQ1JJUFRfTUVUQSB7XCJ0eXBlXCI6XCJTVEFSVFwifVxuZGVmIHJ1bihyZXF1ZXN0KXtcbiAgICByZXR1cm4gcmVxdWVzdC5nZXRTdGFydE5vZGUoKS5nZXRJZCgpO1xufVxuIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLkv53lrZgifSwiaWQiOiJkdEloUkc2TkYzTVRja01LV20iLCJ0eXBlIjoiU0FWRSIsInRpdGxlIjoi5L+d5a2YIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLliqDnrb4ifSwiaWQiOiI3SWFZWVJ5ejVuRGFyUVBlYmEiLCJ0eXBlIjoiQUREX0FVRElUIiwidGl0bGUiOiLliqDnrb4ifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6Iui9rOWKniJ9LCJpZCI6IjkxNEpkQmhVdGNyRTZkTEJkdiIsInR5cGUiOiJUUkFOU0ZFUiIsInRpdGxlIjoi6L2s5YqeIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLpgIDlm54ifSwiaWQiOiJ3Qzc0bjk4RnZqUENjaTM2Y1giLCJ0eXBlIjoiUkVUVVJOIiwidGl0bGUiOiLpgIDlm54ifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IuWnlOa0viJ9LCJpZCI6IktwSEJ3N3NBUlg1Mk1FMUJyVyIsInR5cGUiOiJERUxFR0FURSIsInRpdGxlIjoi5aeU5rS+In1dLCJvcmRlciI6IjAifV0sImRpc3BsYXkiOmZhbHNlLCJuYW1lIjoi5p2h5Lu25YiG5pSv6IqC54K5IiwiaWQiOiJrNXJOT1l0Z0w0QVo0MHhYZmsiLCJ0eXBlIjoiQ09ORElUSU9OX0JSQU5DSCIsImFjdGlvbnMiOnsiJHJlZiI6IiQubm9kZXNbMF0uc3RyYXRlZ2llc1sxXS5maWVsZFBlcm1pc3Npb25zIn0sInNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5aSp5pWwIOWkp+S6juetieS6jiAzXG4gICAgICAgIC8vIEBTQ1JJUFRfTUVUQSB7XCJ2YXJpYWJsZXNcIjpbe1wibGFiZWxcIjpcIuW9k+WJjeWuoeaJueS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Q3VycmVudE9wZXJhdG9yTmFtZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+W9k+WJjeWuoeaJueS6un1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6MX0se1wibGFiZWxcIjpcIuW9k+WJjeWuoeaJueS6uklEXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRDdXJyZW50T3BlcmF0b3JJZCgpXCIsXCJleHByZXNzaW9uXCI6XCIke+W9k+WJjeWuoeaJueS6uklEfVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIkxPTkdcIixcIm9yZGVyXCI6Mn0se1wibGFiZWxcIjpcIua1geeoi+WIm+W7uuS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Q3JlYXRlZE9wZXJhdG9yTmFtZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+a1geeoi+WIm+W7uuS6un1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6M30se1wibGFiZWxcIjpcIua1geeoi+WIm+W7uuS6uklEXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRDcmVhdGVkT3BlcmF0b3JJZCgpXCIsXCJleHByZXNzaW9uXCI6XCIke+a1geeoi+WIm+W7uuS6uklEfVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIkxPTkdcIixcIm9yZGVyXCI6NH0se1wibGFiZWxcIjpcIua1geeoi+aPkOS6pOS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0U3VibWl0T3BlcmF0b3JOYW1lKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL5Yib5bu65Lq6fVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjo1fSx7XCJsYWJlbFwiOlwi5rWB56iL5o+Q5Lqk5Lq6SURcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmdldFN1Ym1pdE9wZXJhdG9ySWQoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvmtYHnqIvmj5DkuqTkurpJRH1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJMT05HXCIsXCJvcmRlclwiOjZ9LHtcImxhYmVsXCI6XCLmmK/lkKbnrqHnkIblkZhcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmlzRmxvd01hbmFnZXIoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvmmK/lkKbnrqHnkIblkZh9XCIsXCJ0eXBlXCI6XCJCT09MRUFOXCIsXCJ0YWdcIjpcIuaTjeS9nOS6uuebuOWFs1wiLFwib3JkZXJcIjo3fSx7XCJsYWJlbFwiOlwi5rWB56iL5qCH6aKYXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRXb3JrZmxvd1RpdGxlKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL5qCH6aKYfVwiLFwidGFnXCI6XCLmtYHnqIvnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjoxMH0se1wibGFiZWxcIjpcIua1geeoi+e8lueggVwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0V29ya2Zsb3dDb2RlKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL57yW56CBfVwiLFwidGFnXCI6XCLmtYHnqIvnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjoxMX0se1wibGFiZWxcIjpcIuW9k+WJjeiKgueCuVwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Tm9kZU5hbWUoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvlvZPliY3oioLngrl9XCIsXCJ0YWdcIjpcIua1geeoi+ebuOWFs1wiLFwidHlwZVwiOlwiU1RSSU5HXCIsXCJvcmRlclwiOjEyfSx7XCJsYWJlbFwiOlwi6IqC54K557G75Z6LXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXROb2RlVHlwZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+iKgueCueexu+Wei31cIixcInRhZ1wiOlwi5rWB56iL55u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6MTN9LHtcImxhYmVsXCI6XCLmmK/lkKbmqKHmi5/mtYvor5VcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmlzTW9jaygpXCIsXCJleHByZXNzaW9uXCI6XCIke+aYr+WQpuaooeaLn+a1i+ivlX1cIixcInR5cGVcIjpcIkJPT0xFQU5cIixcInRhZ1wiOlwi5rWB56iL55u45YWzXCIsXCJvcmRlclwiOjE0fSx7XCJsYWJlbFwiOlwi5aSp5pWwXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGF5cycpXCIsXCJleHByZXNzaW9uXCI6XCIke+WkqeaVsH1cIixcInRhZ1wiOlwi6KGo5Y2V5a2X5q61XCIsXCJvcmRlclwiOjEwMH0se1wibGFiZWxcIjpcIuW8gOWni+aXpeacn1wiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Rm9ybURhdGEoJ3N0YXJ0JylcIixcImV4cHJlc3Npb25cIjpcIiR75byA5aeL5pel5pyffVwiLFwidGFnXCI6XCLooajljZXlrZfmrrVcIixcIm9yZGVyXCI6MTAxfSx7XCJsYWJlbFwiOlwi55CG55SxXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGVzYycpXCIsXCJleHByZXNzaW9uXCI6XCIke+eQhueUsX1cIixcInRhZ1wiOlwi6KGo5Y2V5a2X5q61XCIsXCJvcmRlclwiOjEwMn1dLFwiZ3JvdXBzXCI6W3tcImlkXCI6XCIxeUFWWjBcIixcImxlZnRcIjp7XCJsYWJlbFwiOlwi5aSp5pWwXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGF5cycpXCIsXCJ0eXBlXCI6XCJ2YXJpYWJsZVwifSxcInR5cGVcIjpcImdyZWF0ZXJfZXF1YWxcIixcInJpZ2h0XCI6e1wibGFiZWxcIjpcIjNcIixcInZhbHVlXCI6XCIzXCIsXCJkYXRhVHlwZVwiOlwiU1RSSU5HXCIsXCJ0eXBlXCI6XCJpbnB1dFwifX1dLFwicmVsYXRpb25zXCI6W3tcImlkXCI6XCJ5bmVEemNcIixcInR5cGVcIjpcImNvbmRpdGlvblwiLFwibGFiZWxcIjpcIuWkqeaVsCDlpKfkuo7nrYnkuo4gM1wiLFwiZ3JvdXBJZFwiOlwiMXlBVlowXCJ9XX1cbiAgICAgICAgZGVmIHJ1bihyZXF1ZXN0KXtcbiAgICAgICAgICAgIHJldHVybiByZXF1ZXN0LmdldEZvcm1EYXRhKCdkYXlzJyk+PTM7XG4gICAgICAgIH1cbiAgICAgICAgIiwib3JkZXIiOiIxIn0seyJzdHJhdGVnaWVzIjp7IiRyZWYiOiIkLm5vZGVzWzBdLnN0cmF0ZWdpZXNbMV0uZmllbGRQZXJtaXNzaW9ucyJ9LCJibG9ja3MiOlt7InZpZXciOiJkZWZhdWx0Iiwic3RyYXRlZ2llcyI6W3sidGltZW91dFRpbWUiOiI4NjQwMDAwMCIsInR5cGUiOiJSRU1JTkQiLCJzdHJhdGVneVR5cGUiOiJUaW1lb3V0U3RyYXRlZ3kifSx7InR5cGUiOiJTRVFVRU5DRSIsInBlcmNlbnQiOiIwLjAiLCJzdHJhdGVneVR5cGUiOiJNdWx0aU9wZXJhdG9yQXVkaXRTdHJhdGVneSJ9LHsidHlwZSI6IkFVVE9fUEFTUyIsInN0cmF0ZWd5VHlwZSI6IlNhbWVPcGVyYXRvckF1ZGl0U3RyYXRlZ3kifSx7ImVuYWJsZSI6ZmFsc2UsInN0cmF0ZWd5VHlwZSI6IlJlY29yZE1lcmdlU3RyYXRlZ3kifSx7InR5cGUiOiJSRVNVTUUiLCJzdHJhdGVneVR5cGUiOiJSZXN1Ym1pdFN0cmF0ZWd5In0seyJzaWduUmVxdWlyZWQiOmZhbHNlLCJhZHZpY2VSZXF1aXJlZCI6ZmFsc2UsInN0cmF0ZWd5VHlwZSI6IkFkdmljZVN0cmF0ZWd5In0seyJzY3JpcHQiOiIvLyBAU0NSSVBUX1RJVExFIOWbnumAgOiHs+W8gOWni+iKgueCuVxuLy8gQFNDUklQVF9NRVRBIHtcInR5cGVcIjpcIm5vZGVcIixcIm5vZGVcIjpcIlNUQVJUXCJ9XG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiByZXF1ZXN0LmdldFN0YXJ0Tm9kZSgpLmdldElkKCk7XG59XG4iLCJzdHJhdGVneVR5cGUiOiJFcnJvclRyaWdnZXJTdHJhdGVneSJ9LHsic2NyaXB0IjoiLy8gQFNDUklQVF9USVRMRSDkvaDmnInkuIDmnaHlvoXlip5cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuICfkvaDmnInkuIDmnaHlvoXlip4nXG59XG4iLCJzdHJhdGVneVR5cGUiOiJOb2RlVGl0bGVTdHJhdGVneSJ9LHsic3RyYXRlZ3lUeXBlIjoiRm9ybUZpZWxkUGVybWlzc2lvblN0cmF0ZWd5IiwiZmllbGRQZXJtaXNzaW9ucyI6eyIkcmVmIjoiJC5ub2Rlc1swXS5zdHJhdGVnaWVzWzFdLmZpZWxkUGVybWlzc2lvbnMifX0seyJzY3JpcHQiOiIvLyBAU0NSSVBUX1RJVExFIOa1geeoi+WIm+W7uuiAhVxuLy8gQFNDUklQVF9NRVRBIHtcInR5cGVcIjpcImNyZWF0b3JcIn1cbmRlZiBydW4ocmVxdWVzdCl7XG4gICAgcmV0dXJuIFtyZXF1ZXN0LmdldENyZWF0ZWRPcGVyYXRvcklkKCldXG59XG4iLCJzdHJhdGVneVR5cGUiOiJPcGVyYXRvckxvYWRTdHJhdGVneSJ9LHsiZW5hYmxlIjp0cnVlLCJ0eXBlIjoiUkVWT0tFX0NVUlJFTlQiLCJzdHJhdGVneVR5cGUiOiJSZXZva2VTdHJhdGVneSJ9XSwiZGlzcGxheSI6dHJ1ZSwibmFtZSI6Iue7j+eQhuiKgueCuSIsImlkIjoiS2NqUG1KZm1iUmU1V2UwSlZNIiwidHlwZSI6IkFQUFJPVkFMIiwiYWN0aW9ucyI6W3siZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi6YCa6L+HIn0sImlkIjoiY0RFeXR1Ukd4Nm82bXNDMFI2IiwidHlwZSI6IlBBU1MiLCJ0aXRsZSI6IumAmui/hyJ9LHsiZW5hYmxlIjp0cnVlLCJkaXNwbGF5Ijp7InRpdGxlIjoi5ouS57udIn0sImlkIjoiUm9oczN4OWpyU2tyRmhwNzFjIiwidHlwZSI6IlJFSkVDVCIsInRpdGxlIjoi5ouS57udIiwic2NyaXB0IjoiLy8gQFNDUklQVF9USVRMRSDov5Tlm57lvIDlp4voioLngrlcbi8vIEBTQ1JJUFRfTUVUQSB7XCJ0eXBlXCI6XCJTVEFSVFwifVxuZGVmIHJ1bihyZXF1ZXN0KXtcbiAgICByZXR1cm4gcmVxdWVzdC5nZXRTdGFydE5vZGUoKS5nZXRJZCgpO1xufVxuIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLkv53lrZgifSwiaWQiOiJUTkgyZ0UwNmhXUVBzSEJZMkEiLCJ0eXBlIjoiU0FWRSIsInRpdGxlIjoi5L+d5a2YIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLliqDnrb4ifSwiaWQiOiJDaW9YclRDdHNrbjExQ1poaWoiLCJ0eXBlIjoiQUREX0FVRElUIiwidGl0bGUiOiLliqDnrb4ifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6Iui9rOWKniJ9LCJpZCI6InVZWlh3OGI0Y1dESlEyTmdDUyIsInR5cGUiOiJUUkFOU0ZFUiIsInRpdGxlIjoi6L2s5YqeIn0seyJlbmFibGUiOnRydWUsImRpc3BsYXkiOnsidGl0bGUiOiLpgIDlm54ifSwiaWQiOiJwMzQ0TVlMTzVPZXNMdVFRRlAiLCJ0eXBlIjoiUkVUVVJOIiwidGl0bGUiOiLpgIDlm54ifSx7ImVuYWJsZSI6dHJ1ZSwiZGlzcGxheSI6eyJ0aXRsZSI6IuWnlOa0viJ9LCJpZCI6IjBEdWhuWVNRYldIUEk3SlhaNSIsInR5cGUiOiJERUxFR0FURSIsInRpdGxlIjoi5aeU5rS+In1dLCJvcmRlciI6IjAifV0sImRpc3BsYXkiOmZhbHNlLCJuYW1lIjoi5p2h5Lu25YiG5pSv6IqC54K5IiwiaWQiOiJRd2NvakdBMzBsbHFFYnJuVXAiLCJ0eXBlIjoiQ09ORElUSU9OX0JSQU5DSCIsImFjdGlvbnMiOnsiJHJlZiI6IiQubm9kZXNbMF0uc3RyYXRlZ2llc1sxXS5maWVsZFBlcm1pc3Npb25zIn0sInNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5aSp5pWwIOWwj+S6jiAzXG4gICAgICAgIC8vIEBTQ1JJUFRfTUVUQSB7XCJ2YXJpYWJsZXNcIjpbe1wibGFiZWxcIjpcIuW9k+WJjeWuoeaJueS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Q3VycmVudE9wZXJhdG9yTmFtZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+W9k+WJjeWuoeaJueS6un1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6MX0se1wibGFiZWxcIjpcIuW9k+WJjeWuoeaJueS6uklEXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRDdXJyZW50T3BlcmF0b3JJZCgpXCIsXCJleHByZXNzaW9uXCI6XCIke+W9k+WJjeWuoeaJueS6uklEfVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIkxPTkdcIixcIm9yZGVyXCI6Mn0se1wibGFiZWxcIjpcIua1geeoi+WIm+W7uuS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Q3JlYXRlZE9wZXJhdG9yTmFtZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+a1geeoi+WIm+W7uuS6un1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6M30se1wibGFiZWxcIjpcIua1geeoi+WIm+W7uuS6uklEXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRDcmVhdGVkT3BlcmF0b3JJZCgpXCIsXCJleHByZXNzaW9uXCI6XCIke+a1geeoi+WIm+W7uuS6uklEfVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIkxPTkdcIixcIm9yZGVyXCI6NH0se1wibGFiZWxcIjpcIua1geeoi+aPkOS6pOS6ulwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0U3VibWl0T3BlcmF0b3JOYW1lKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL5Yib5bu65Lq6fVwiLFwidGFnXCI6XCLmk43kvZzkurrnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjo1fSx7XCJsYWJlbFwiOlwi5rWB56iL5o+Q5Lqk5Lq6SURcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmdldFN1Ym1pdE9wZXJhdG9ySWQoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvmtYHnqIvmj5DkuqTkurpJRH1cIixcInRhZ1wiOlwi5pON5L2c5Lq655u45YWzXCIsXCJ0eXBlXCI6XCJMT05HXCIsXCJvcmRlclwiOjZ9LHtcImxhYmVsXCI6XCLmmK/lkKbnrqHnkIblkZhcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmlzRmxvd01hbmFnZXIoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvmmK/lkKbnrqHnkIblkZh9XCIsXCJ0eXBlXCI6XCJCT09MRUFOXCIsXCJ0YWdcIjpcIuaTjeS9nOS6uuebuOWFs1wiLFwib3JkZXJcIjo3fSx7XCJsYWJlbFwiOlwi5rWB56iL5qCH6aKYXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRXb3JrZmxvd1RpdGxlKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL5qCH6aKYfVwiLFwidGFnXCI6XCLmtYHnqIvnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjoxMH0se1wibGFiZWxcIjpcIua1geeoi+e8lueggVwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0V29ya2Zsb3dDb2RlKClcIixcImV4cHJlc3Npb25cIjpcIiR75rWB56iL57yW56CBfVwiLFwidGFnXCI6XCLmtYHnqIvnm7jlhbNcIixcInR5cGVcIjpcIlNUUklOR1wiLFwib3JkZXJcIjoxMX0se1wibGFiZWxcIjpcIuW9k+WJjeiKgueCuVwiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Tm9kZU5hbWUoKVwiLFwiZXhwcmVzc2lvblwiOlwiJHvlvZPliY3oioLngrl9XCIsXCJ0YWdcIjpcIua1geeoi+ebuOWFs1wiLFwidHlwZVwiOlwiU1RSSU5HXCIsXCJvcmRlclwiOjEyfSx7XCJsYWJlbFwiOlwi6IqC54K557G75Z6LXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXROb2RlVHlwZSgpXCIsXCJleHByZXNzaW9uXCI6XCIke+iKgueCueexu+Wei31cIixcInRhZ1wiOlwi5rWB56iL55u45YWzXCIsXCJ0eXBlXCI6XCJTVFJJTkdcIixcIm9yZGVyXCI6MTN9LHtcImxhYmVsXCI6XCLmmK/lkKbmqKHmi5/mtYvor5VcIixcInZhbHVlXCI6XCJyZXF1ZXN0LmlzTW9jaygpXCIsXCJleHByZXNzaW9uXCI6XCIke+aYr+WQpuaooeaLn+a1i+ivlX1cIixcInR5cGVcIjpcIkJPT0xFQU5cIixcInRhZ1wiOlwi5rWB56iL55u45YWzXCIsXCJvcmRlclwiOjE0fSx7XCJsYWJlbFwiOlwi5aSp5pWwXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGF5cycpXCIsXCJleHByZXNzaW9uXCI6XCIke+WkqeaVsH1cIixcInRhZ1wiOlwi6KGo5Y2V5a2X5q61XCIsXCJvcmRlclwiOjEwMH0se1wibGFiZWxcIjpcIuW8gOWni+aXpeacn1wiLFwidmFsdWVcIjpcInJlcXVlc3QuZ2V0Rm9ybURhdGEoJ3N0YXJ0JylcIixcImV4cHJlc3Npb25cIjpcIiR75byA5aeL5pel5pyffVwiLFwidGFnXCI6XCLooajljZXlrZfmrrVcIixcIm9yZGVyXCI6MTAxfSx7XCJsYWJlbFwiOlwi55CG55SxXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGVzYycpXCIsXCJleHByZXNzaW9uXCI6XCIke+eQhueUsX1cIixcInRhZ1wiOlwi6KGo5Y2V5a2X5q61XCIsXCJvcmRlclwiOjEwMn1dLFwiZ3JvdXBzXCI6W3tcImlkXCI6XCI1Zi12aDlcIixcImxlZnRcIjp7XCJsYWJlbFwiOlwi5aSp5pWwXCIsXCJ2YWx1ZVwiOlwicmVxdWVzdC5nZXRGb3JtRGF0YSgnZGF5cycpXCIsXCJ0eXBlXCI6XCJ2YXJpYWJsZVwifSxcInR5cGVcIjpcImxlc3NfdGhhblwiLFwicmlnaHRcIjp7XCJsYWJlbFwiOlwiM1wiLFwidmFsdWVcIjpcIjNcIixcImRhdGFUeXBlXCI6XCJTVFJJTkdcIixcInR5cGVcIjpcImlucHV0XCJ9fV0sXCJyZWxhdGlvbnNcIjpbe1wiaWRcIjpcIk91c0lWbVwiLFwidHlwZVwiOlwiY29uZGl0aW9uXCIsXCJsYWJlbFwiOlwi5aSp5pWwIOWwj+S6jiAzXCIsXCJncm91cElkXCI6XCI1Zi12aDlcIn1dfVxuICAgICAgICBkZWYgcnVuKHJlcXVlc3Qpe1xuICAgICAgICAgICAgcmV0dXJuIHJlcXVlc3QuZ2V0Rm9ybURhdGEoJ2RheXMnKTwzO1xuICAgICAgICB9XG4gICAgICAgICIsIm9yZGVyIjoiMiJ9XSwiZGlzcGxheSI6ZmFsc2UsIm5hbWUiOiLmnaHku7bmjqfliLboioLngrkiLCJpZCI6Ik8yd1d3ajdHZmF0ZTQwd096RiIsInR5cGUiOiJDT05ESVRJT04iLCJhY3Rpb25zIjp7IiRyZWYiOiIkLm5vZGVzWzBdLnN0cmF0ZWdpZXNbMV0uZmllbGRQZXJtaXNzaW9ucyJ9LCJvcmRlciI6IjAifSx7InN0cmF0ZWdpZXMiOnsiJHJlZiI6IiQubm9kZXNbMF0uc3RyYXRlZ2llc1sxXS5maWVsZFBlcm1pc3Npb25zIn0sImRpc3BsYXkiOnRydWUsIm5hbWUiOiLnu5PmnZ/oioLngrkiLCJpZCI6InUxVXhqUnFFbTlNVFBuM2szQyIsInR5cGUiOiJFTkQiLCJhY3Rpb25zIjp7IiRyZWYiOiIkLm5vZGVzWzBdLnN0cmF0ZWdpZXNbMV0uZmllbGRQZXJtaXNzaW9ucyJ9LCJvcmRlciI6IjAifV0sImZvcm0iOnsiY29kZSI6ImxlYXZlIiwibmFtZSI6Iuivt+WBh+WNlSIsImZpZWxkcyI6W3siY29kZSI6ImRheXMiLCJoaWRkZW4iOmZhbHNlLCJkYXRhVHlwZSI6IklOVEVHRVIiLCJuYW1lIjoi5aSp5pWwIiwiYXR0cmlidXRlcyI6W10sImlkIjoiYzUyYWU5NTEtMDk3Mi00NWEyLWEzZTYtZmVlZmY1NGIyZDk5IiwicGxhY2Vob2xkZXIiOiLor7fovpPlhaXlpKnmlbAiLCJ0eXBlIjoiaW50ZWdlciIsInJlcXVpcmVkIjp0cnVlfSx7ImNvZGUiOiJzdGFydCIsImhpZGRlbiI6ZmFsc2UsImRhdGFUeXBlIjoiU1RSSU5HIiwibmFtZSI6IuW8gOWni+aXpeacnyIsImF0dHJpYnV0ZXMiOltdLCJpZCI6IjU2MjYyMDhlLWM3NWItNDgwMy05MGI2LWNkOWIwMTBkYTMyYiIsInBsYWNlaG9sZGVyIjoi6K+36YCJ5oup5pel5pyfIiwidHlwZSI6InN0cmluZyIsInJlcXVpcmVkIjp0cnVlfSx7ImNvZGUiOiJkZXNjIiwiaGlkZGVuIjpmYWxzZSwiZGF0YVR5cGUiOiJTVFJJTkciLCJuYW1lIjoi55CG55SxIiwiYXR0cmlidXRlcyI6W10sImlkIjoiMGM5YmIxYTctZmU1ZS00YjQ4LWEzYWYtNTg2MTczNjVkZTZmIiwicGxhY2Vob2xkZXIiOiLor7fovpPlhaXnkIbnlLEiLCJ0eXBlIjoic3RyaW5nIiwicmVxdWlyZWQiOnRydWV9XX0sImNyZWF0ZWRPcGVyYXRvciI6IjEiLCJzdHJhdGVnaWVzIjpbeyJlbmFibGUiOnRydWUsInN0cmF0ZWd5VHlwZSI6IkludGVyZmVyZVN0cmF0ZWd5In0seyJlbmFibGUiOnRydWUsImludGVydmFsIjoiNjAiLCJzdHJhdGVneVR5cGUiOiJVcmdlU3RyYXRlZ3kifV0sImNyZWF0ZWRUaW1lIjoiMTc3MzQxNjU5MTQ4NiIsImlkIjoiam00b0l4Mmp5allrRk9CQ2hIIiwidGl0bGUiOiLor7flgYfmtYHnqIsiLCJvcGVyYXRvckNyZWF0ZVNjcmlwdCI6Ii8vIEBTQ1JJUFRfVElUTEUg5Lu75oSP55So5oi3XG4vLyBAU0NSSVBUX01FVEEge1widHlwZVwiOlwiYW55XCJ9XG5kZWYgcnVuKHJlcXVlc3Qpe1xuICAgIHJldHVybiB0cnVlXG59XG4ifQ==";
        String workId = factory.workflowService.importWorkflow(data, user);
        Workflow workflow = factory.workflowService.getWorkflow(workId);
        assertEquals("leave", workflow.getForm().getCode());
        assertEquals("请假流程", workflow.getTitle());
    }


    /**
     * 全部通过测试
     */
    @Test
    void pass() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        assertEquals(user.getUserId(), bossRecordList.get(0).getSubmitOperatorId());
        assertEquals(user.getName(), bossRecordList.get(0).getSubmitOperatorName());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 办理节点测试
     */
    @Test
    void handle() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        HandleNode bossNode = HandleNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 抄送节点测试
     */
    @Test
    void notifyNode() {
        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        NotifyNode bossNode = NotifyNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), null, user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 条件分支测试 else的情况
     */
    @Test
    void conditionElse() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') == 3}")
                .order(1)
                .blocks(departApprovalNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') == 5}")
                .order(2)
                .blocks(bossApprovalNode)
                .build();

        ConditionElseBranchNode elseConditionNode = ConditionElseBranchNode.builder()
                .name("else条件分支")
                .order(3)
                .blocks(departApprovalNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制")
                .blocks(departConditionNode, bossConditionNode, elseConditionNode)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 4, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 条件分支测试
     */
    @Test
    void condition() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .blocks(departApprovalNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .blocks(bossApprovalNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制")
                .blocks(departConditionNode, bossConditionNode)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 拒绝测试
     */
    @Test
    void reject() {

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);
        FlowScriptContext.getInstance().setBeanFactory(new IBeanFactory() {
            @Override
            public FlowRecord getRecordById(long id) {
                return factory.flowRecordRepository.get(id);
            }
        });

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(1).id(), "不同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> userToDoList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userToDoList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userToDoList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());
    }


    /**
     * 并行分支测试
     */
    @Test
    void parallel() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();


        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ParallelBranchNode parallelBranchNode1 = ParallelBranchNode.builder()
                .name("并行分支1")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        ParallelBranchNode parallelBranchNode2 = ParallelBranchNode.builder()
                .name("并行分支2")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        ParallelNode parallelNode = ParallelNode.builder()
                .name("并行控制节点")
                .blocks(parallelBranchNode1, parallelBranchNode2)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(parallelNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<FlowRecord> boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 人工分支测试
     */
    @Test
    void manual() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();


        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        ManualBranchNode manualBranchNode1 = ManualBranchNode.builder()
                .name("人工分支1")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        ManualBranchNode manualBranchNode2 = ManualBranchNode.builder()
                .name("人工分支2")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        ManualNode manualNode = ManualNode.builder()
                .name("人工控制节点")
                .blocks(manualBranchNode1, manualBranchNode2)
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(manualNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        FlowAdviceBody adviceBody = new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId());
        userRequest.setAdvice(adviceBody);
        ActionResponse actionResponse = factory.flowService.action(userRequest);
        assertEquals(2, actionResponse.getOptions().size());

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        adviceBody.setManualNodeId(actionResponse.getOptions().get(0).getId());
        actionResponse = factory.flowService.action(userRequest);
        assertNull(actionResponse);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        actionResponse = factory.flowService.action(departRequest);
        assertNull(actionResponse);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 包容分支测试
     */
    @Test
    void inclusive() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();


        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        InclusiveBranchNode parallelBranchNode1 = InclusiveBranchNode.builder()
                .name("包容分支1")
                .conditionScript("def run(request){return true}")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        InclusiveBranchNode parallelBranchNode2 = InclusiveBranchNode.builder()
                .name("包容分支2")
                .conditionScript("def run(request){return request.getFormData('days') >= 3}")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        InclusiveNode inclusiveNode = InclusiveNode.builder()
                .name("包容控制")
                .blocks(parallelBranchNode1, parallelBranchNode2)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(inclusiveNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 3, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());

        List<FlowRecord> boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());


        List<IFlowAction> departActions = departApprovalNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);

        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bossActions = bossApprovalNode.actionManager().getActions();

        FlowActionRequest dossRequest = new FlowActionRequest();
        dossRequest.setFormData(data);
        dossRequest.setRecordId(boosRecordList.get(0).getId());
        dossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(dossRequest);


        boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(departRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 包容分支测试
     */
    @Test
    void inclusiveElse() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();


        ApprovalNode departApprovalNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        ApprovalNode bossApprovalNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        ApprovalNode bigBossApprovalNode = ApprovalNode.builder()
                .name("大老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();


        InclusiveBranchNode inclusiveBranchNode1 = InclusiveBranchNode.builder()
                .name("包容分支1")
                .conditionScript("def run(request){return request.getFormData('days') == 3}")
                .blocks(departApprovalNode)
                .order(1)
                .build();

        InclusiveBranchNode inclusiveBranchNode2 = InclusiveBranchNode.builder()
                .name("包容分支2")
                .conditionScript("def run(request){return request.getFormData('days') == 5}")
                .blocks(bossApprovalNode, bigBossApprovalNode)
                .order(2)
                .build();

        InclusiveElseBranchNode inclusiveElseBranchNode = InclusiveElseBranchNode.builder()
                .name("包容else分支")
                .blocks(bigBossApprovalNode)
                .order(3)
                .build();

        InclusiveNode inclusiveNode = InclusiveNode.builder()
                .name("包容控制")
                .blocks(inclusiveBranchNode1, inclusiveBranchNode2, inclusiveElseBranchNode)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(inclusiveNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 4, "reason", "leave");
        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);


        List<FlowRecord>  boosRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, boosRecordList.size());

        List<IFlowAction> bigBossActions = bigBossApprovalNode.actionManager().getActions();

        FlowActionRequest bigBossRequest = new FlowActionRequest();
        bigBossRequest.setFormData(data);
        bigBossRequest.setRecordId(boosRecordList.get(0).getId());
        bigBossRequest.setAdvice(new FlowAdviceBody(bigBossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bigBossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(boosRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isTodo).toList().size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 路由节点测试
     */
    @Test
    void router() {

        User user = new User(1, "user");
        User depart = new User(2, "depart");
        User boss = new User(3, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(depart);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();


        ApprovalNode bossNode = ApprovalNode.builder()
                .name("老板审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [3]}"))
                        .build()
                )
                .build();

        RouterNode routerNode = RouterNode.builder()
                .name("路由节点")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new RouterStrategy(String.format("def run(request){return '%s'}", bossNode.getId())))
                        .build())
                .build();

        ApprovalNode departNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();


        ConditionBranchNode departConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') <= 3}")
                .order(1)
                .blocks(departNode, routerNode)
                .build();

        ConditionBranchNode bossConditionNode = ConditionBranchNode.builder()
                .name("条件分支")
                .conditionScript("def run(request){return request.getFormData('days') > 3}")
                .order(2)
                .blocks(bossNode)
                .build();

        ConditionNode conditionNode = ConditionNode.builder()
                .name("条件控制节点")
                .blocks(departConditionNode, bossConditionNode)
                .build();


        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(conditionNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 2, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);

        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest submitRequest = new FlowActionRequest();
        submitRequest.setFormData(data);
        submitRequest.setRecordId(userRecordList.get(0).getId());
        submitRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(submitRequest);

        List<FlowRecord> departRecordList = factory.flowRecordRepository.findTodoByOperator(depart.getUserId());
        assertEquals(1, departRecordList.size());


        List<IFlowAction> departActions = departNode.actionManager().getActions();

        FlowActionRequest departRequest = new FlowActionRequest();
        departRequest.setFormData(data);
        departRequest.setRecordId(departRecordList.get(0).getId());
        departRequest.setAdvice(new FlowAdviceBody(departActions.get(0).id(), "同意", depart.getUserId()));
        factory.flowService.action(departRequest);


        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 全部通过测试
     */
    @Test
    void delay() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        DelayNode delayNode = DelayNode.builder()
                .name("延迟节点")
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(delayNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());

        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());
        try {
            // 默认等待时间为5秒
            Thread.sleep(8000);
        } catch (Exception ignore) {
        }

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 触发节点测试
     */
    @Test
    void trigger() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();


        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        TriggerNode triggerNode = TriggerNode.builder()
                .name("触发流程")
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(triggerNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 子流程节点测试
     */
    @Test
    void subProcess() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        SubProcessNode subProcessNode = SubProcessNode.builder()
                .name("子流程")
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(subProcessNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = userCreateRequest.toActionRequest(userRecordList.get(0).getId());
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

        // 为老板再次创建一个待办流程
        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

    }


    /**
     * 保存测试
     */
    @Test
    void save() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        // 保存数据
        data.put("reason", "test");
        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(1).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        Map<String, Object> currentFormData = userRecordList.get(0).getFormData();
        assertEquals("test", currentFormData.get("reason"));

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 加签测试
     */
    @Test
    void addAudit() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest addAuditRequest = new FlowActionRequest();
        addAuditRequest.setFormData(data);
        addAuditRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody addAuditAdviceBody = new FlowAdviceBody(bossActions.get(3).id(), boss.getUserId());
        addAuditAdviceBody.setForwardOperatorIds(List.of(3L));
        addAuditRequest.setAdvice(addAuditAdviceBody);
        factory.flowService.action(addAuditRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(0, lorneRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 转办测试
     */
    @Test
    void transfer() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest transferRequest = new FlowActionRequest();
        transferRequest.setFormData(data);
        transferRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody transferAdviceBody = new FlowAdviceBody(bossActions.get(4).id(), boss.getUserId());
        transferAdviceBody.setForwardOperatorIds(List.of(3L));
        transferRequest.setAdvice(transferAdviceBody);
        factory.flowService.action(transferRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 退回节点测试
     */
    @Test
    void returnNode() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest returnRequest = new FlowActionRequest();
        returnRequest.setFormData(data);
        returnRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody backAdviceBody = new FlowAdviceBody(bossActions.get(5).id(), boss.getUserId());
        backAdviceBody.setBackNodeId(startNode.getId());
        returnRequest.setAdvice(backAdviceBody);
        factory.flowService.action(returnRequest);

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 委托测试
     */
    @Test
    void delegate() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest delegateRequest = new FlowActionRequest();
        delegateRequest.setFormData(data);
        delegateRequest.setRecordId(bossRecordList.get(0).getId());

        FlowAdviceBody delegateAdviceBody = new FlowAdviceBody(bossActions.get(6).id(), boss.getUserId());
        delegateAdviceBody.setForwardOperatorIds(List.of(lorne.getUserId()));
        delegateRequest.setAdvice(delegateAdviceBody);
        factory.flowService.action(delegateRequest);

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(4, records.size());
        assertEquals(4, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 自定义事件测试
     */
    @Test
    void custom() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = new HashMap<>(Map.of("name", "lorne", "days", 1, "reason", "leave"));

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(7).id(), boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 撤回测试
     */
    @Test
    void revoke() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = factory.flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        factory.flowService.revoke(new FlowRevokeRequest(userDoneList.get(0).getId(), user.getUserId()));

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 催办测试
     */
    @Test
    void urge() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        factory.userGateway.save(user);
        factory.userGateway.save(boss);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());

        List<FlowRecord> userDoneList = factory.flowRecordRepository.findDoneByOperator(user.getUserId());
        assertEquals(1, userDoneList.size());

        factory.flowService.urge(new FlowUrgeRequest(userDoneList.get(0).getId(), user.getUserId()));

        bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", boss.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 流程干预测试
     */
    @Test
    void interfere() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne", true);

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(1, bossRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(bossRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(bossRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 用户转交审批测试
     */
    @Test
    void forwardOperator() {

        User lorne = new User(3, "lorne");
        User user = new User(1, "user");
        // 老板将审批权转给了lorne账户
        User boss = new User(2, "boss", lorne);

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [2]}"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest lorneRequest = new FlowActionRequest();
        lorneRequest.setFormData(data);
        lorneRequest.setRecordId(lorneRecordList.get(0).getId());
        lorneRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(lorneRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(3, records.size());
        assertEquals(3, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 节点异常测试
     */
    @Test
    void errorOperatorTest() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [-1]}"))
                        .addStrategy(new ErrorTriggerStrategy("def run(request){ return 3; }"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        List<FlowRecord> lorneRecordList = factory.flowRecordRepository.findTodoByOperator(lorne.getUserId());
        assertEquals(1, lorneRecordList.size());


        List<IFlowAction> bossActions = bossNode.actionManager().getActions();

        FlowActionRequest bossRequest = new FlowActionRequest();
        bossRequest.setFormData(data);
        bossRequest.setRecordId(lorneRecordList.get(0).getId());
        bossRequest.setAdvice(new FlowAdviceBody(bossActions.get(0).id(), "同意", lorne.getUserId()));
        factory.flowService.action(bossRequest);

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(lorneRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(2, records.stream().filter(FlowRecord::isFinish).toList().size());

    }


    /**
     * 节点异常测试
     */
    @Test
    void errorNodeTest() {

        User user = new User(1, "user");
        User boss = new User(2, "boss");
        User lorne = new User(3, "lorne");

        factory.userGateway.save(user);
        factory.userGateway.save(boss);
        factory.userGateway.save(lorne);

        GatewayContext.getInstance().setFlowOperatorGateway(factory.userGateway);

        FlowForm form = FlowFormBuilder.builder()
                .name("请假流程")
                .code("leave")
                .addField("请假人", "name", DataType.STRING)
                .addField("请假天数", "days", DataType.INTEGER)
                .addField("请假事由", "reason", DataType.STRING)
                .build();

        StartNode startNode = StartNode
                .builder()
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .build())
                .actions(ActionBuilder.builder()
                        .addAction(new CustomAction())
                        .build())
                .build();

        ApprovalNode bossNode = ApprovalNode.builder()
                .name("经理审批")
                .strategies(NodeStrategyBuilder.builder()
                        .addStrategy(new FormFieldPermissionStrategy(FormFieldPermissionsBuilder.builder()
                                .addPermission("leave", "name", PermissionType.WRITE)
                                .addPermission("leave", "days", PermissionType.WRITE)
                                .addPermission("leave", "reason", PermissionType.WRITE)
                                .build()))
                        .addStrategy(new OperatorLoadStrategy("def run(request){return [-1]}"))
                        .addStrategy(new ErrorTriggerStrategy("def run(request){ return '" + startNode.getId() + "'; }"))
                        .build()
                )
                .build();

        EndNode endNode = EndNode.builder().build();
        Workflow workflow = WorkflowBuilder.builder()
                .title("请假流程")
                .code("leave")
                .createdOperator(user)
                .form(form)
                .addNode(startNode)
                .addNode(bossNode)
                .addNode(endNode)
                .build();

        factory.workflowService.saveWorkflow(workflow);

        Map<String, Object> data = Map.of("name", "lorne", "days", 1, "reason", "leave");

        List<IFlowAction> startActions = startNode.actionManager().getActions();
        FlowCreateRequest userCreateRequest = new FlowCreateRequest();
        userCreateRequest.setWorkId(workflow.getId());
        userCreateRequest.setFormData(data);
        userCreateRequest.setActionId(startActions.get(0).id());
        userCreateRequest.setOperatorId(user.getUserId());
        factory.flowService.create(userCreateRequest);

        List<FlowRecord> userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        FlowActionRequest userRequest = new FlowActionRequest();
        userRequest.setFormData(data);
        userRequest.setRecordId(userRecordList.get(0).getId());
        userRequest.setAdvice(new FlowAdviceBody(startActions.get(0).id(), "同意", user.getUserId()));
        factory.flowService.action(userRequest);

        List<FlowRecord> bossRecordList = factory.flowRecordRepository.findTodoByOperator(boss.getUserId());
        assertEquals(0, bossRecordList.size());

        userRecordList = factory.flowRecordRepository.findTodoByOperator(user.getUserId());
        assertEquals(1, userRecordList.size());

        List<FlowRecord> records = factory.flowRecordRepository.findProcessRecords(userRecordList.get(0).getProcessId());
        assertEquals(2, records.size());
        assertEquals(0, records.stream().filter(FlowRecord::isFinish).toList().size());

    }
}