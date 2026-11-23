package com.codingapi.flow.user;

import com.codingapi.flow.operator.IFlowOperator;
import com.codingapi.flow.operator.OperatorIdentity;
import com.codingapi.flow.script.request.GroovyScriptRequest;

import java.util.Map;

public class User implements IFlowOperator {

    private final long userId;
    private final String name;
    private final boolean manager;
    private final User forwardOperator;
    /**
     * 身份标识（issue #288），为空表示默认身份
     */
    private final String identity;

    public User(long userId, String name) {
        this(userId, name, false, null, "");
    }

    public User(long userId, String name, boolean manager) {
        this(userId, name, manager, null, "");
    }

    public User(long userId, String name, User forwardOperator) {
        this(userId, name, false, forwardOperator, "");
    }

    public User(long userId, String name, boolean manager, User forwardOperator) {
        this(userId, name, manager, forwardOperator, "");
    }

    protected User(long userId, String name, boolean manager, User forwardOperator, String identity) {
        this.userId = userId;
        this.name = name;
        this.manager = manager;
        this.forwardOperator = forwardOperator;
        this.identity = identity;
    }

    /**
     * 创建带身份标识的用户（测试多身份场景）
     */
    public static User withIdentity(long userId, String name, String identity) {
        return new User(userId, name, false, null, identity);
    }

    /**
     * 返回同一用户以指定身份参与的副本（保留管理员、转交人等属性）
     */
    public User asIdentity(String identity) {
        return new User(userId, name, manager, forwardOperator, OperatorIdentity.normalize(identity));
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isFlowManager() {
        return manager;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public Map<String, String> getIdentityAttributes() {
        return identity == null || identity.isEmpty() ? Map.of() : Map.of("identityName", identity);
    }

    @Override
    public IFlowOperator forwardOperator(GroovyScriptRequest request) {
        return forwardOperator;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUserId() == userId;
    }
}
