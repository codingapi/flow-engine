package com.codingapi.example.entity;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.script.request.GroovyScriptRequest;
import com.codingapi.springboot.script.annotation.ScriptField;
import com.codingapi.springboot.script.annotation.ScriptFunction;
import com.codingapi.springboot.script.annotation.ScriptType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    /**
     * 当前身份标识（issue #288）。
     *
     * <p>同一个人员可能隶属多个部门/岗位，以不同身份参与流程。身份是**运行期选择**而非用户主数据，
     * 因此不落库；由 {@code FlowOperatorGateway#get(id, identity)} 按身份返回携带该标识的用户副本。
     * 不参与 {@code equals/hashCode}，避免同一 id 的两个身份副本被判定为不同用户。
     */
    @Transient
    @EqualsAndHashCode.Exclude
    @ScriptField(name = "identity", description = "当前身份标识")
    private String identity;


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
        return flowManager!=null && flowManager ;
    }

    /**
     * 当前身份标识，空串表示默认身份（issue #288）。
     */
    @Override
    @ScriptFunction(name = "getIdentity", description = "获取当前身份标识")
    public String getIdentity() {
        return identity;
    }

    /**
     * 返回携带指定身份的副本。
     *
     * <p>必须复制而不是直接改写实体：JPA 返回的是持久化上下文中的共享实例，
     * 直接写入 {@code identity} 会让同一 id 的两个身份互相覆盖（缓存串味）。
     *
     * @param identity 身份标识
     * @return 带身份的副本
     */
    public User withIdentity(String identity) {
        User copy = new User();
        copy.id = this.id;
        copy.name = this.name;
        copy.flowManager = this.flowManager;
        copy.flowOperatorId = this.flowOperatorId;
        copy.account = this.account;
        copy.password = this.password;
        copy.identity = identity;
        return copy;
    }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        if (flowOperatorId != null && flowOperatorId > 0) {
            return GatewayContext.getInstance().getFlowOperator(flowOperatorId);
        }
        return null;
    }
}
