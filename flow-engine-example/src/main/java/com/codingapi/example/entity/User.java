package com.codingapi.example.entity;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.springboot.script.annotation.ScriptField;
import com.codingapi.springboot.script.annotation.ScriptFunction;
import com.codingapi.springboot.script.annotation.ScriptType;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Data
@Entity
@Table(name = "t_user")
@ScriptType(description = "用户信息")
public class User implements IFlowOperator {

    public static final String ADMIN_ROLE = "ROLE_ADMIN";
    public static final String ADMIN_ACCOUNT = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ScriptField(name = "id",description = "用户id")
    private Long id;
    @ScriptField(name = "name",description = "用户姓名")
    private String name;
    @ScriptField(name = "flowManager",description = "是否流程管理员")
    private Boolean flowManager;
    @ScriptField(name = "flowOperatorId",description = "转交审批人Id")
    private Long flowOperatorId;

    @Column(unique = true)
    private String account;
    private String password;


    public static User admin(PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setName(ADMIN_ACCOUNT);
        user.setAccount(ADMIN_ACCOUNT);
        user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        user.setFlowManager(true);
        return user;
    }


    public void encodePassword(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(password);
    }


    public List<String> getRoles() {
        return List.of(ADMIN_ROLE);
    }

    @Override
    @ScriptFunction(name = "getUserId",description = "获取用户id")
    public long getUserId() {
        return id;
    }

    @Override
    @ScriptFunction(name = "getName",description = "获取用户名称")
    public String getName() {
        return name;
    }

    @Override
    @ScriptFunction(name = "isFlowManager",description = "是否为流程管理员")
    public boolean isFlowManager() {
        return flowManager;
    }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        if (flowOperatorId != null && flowOperatorId > 0) {
            return GatewayContext.getInstance().getFlowOperator(flowOperatorId);
        }
        return null;
    }
}
