package com.codingapi.example.entity;

import com.codingapi.flow.context.GatewayContext;
import com.codingapi.flow.operator.IFlowOperator;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Data
@Entity
@Table(name = "t_user")
public class User implements IFlowOperator {

    public static final String ADMIN_ROLE = "ROLE_ADMIN";
    public static final String ADMIN_ACCOUNT = "admin";
    public static final String ADMIN_PASSWORD = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Boolean flowManager;
    private Long flowOperatorId;

    @Column(unique = true)
    private String account;
    private String password;


    public static User admin(PasswordEncoder passwordEncoder){
        User user = new User();
        user.setName(ADMIN_ACCOUNT);
        user.setAccount(ADMIN_ACCOUNT);
        user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        user.setFlowManager(true);
        return user;
    }


    public List<String> getRoles() {
        return List.of(ADMIN_ROLE);
    }

    @Override
    public long getUserId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFlowManager() {
        return flowManager;
    }

    @Override
    public IFlowOperator forwardOperator() {
        return GatewayContext.getInstance().getFlowOperator(flowOperatorId);
    }
}
