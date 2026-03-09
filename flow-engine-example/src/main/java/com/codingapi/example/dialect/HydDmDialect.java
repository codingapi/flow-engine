package com.codingapi.example.dialect;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DmDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;

public class HydDmDialect extends DmDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
        functionFactory.aggregates(this, SqlAstNodeRenderingMode.DEFAULT);
    }
}

