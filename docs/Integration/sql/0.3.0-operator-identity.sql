-- =============================================================================
-- flow-engine 0.3.0 升级脚本：操作者身份标识（issue #288）
-- =============================================================================
-- 背景：
--   同一人员可同时隶属多个部门/岗位，以不同「身份」参与同一个流程。
--   0.3.0 起框架在记录与待办上保存身份标识（不解释语义，由下游定义取值）。
--
-- 兼容性：
--   1. 仅新增 nullable 列，不修改、不删除任何既有列，旧版本程序不映射新列，可安全回滚。
--   2. 历史数据的新列为 NULL，引擎按「null 与空串等价 = 默认身份」读取，无需数据回填。
--   3. 待办合并键在默认身份下保持历史格式，仅非空身份追加 "#identity" 后缀，
--      升级瞬间仍在运行的流程不会出现待办重复或合并关系断裂。
--
-- 使用方式：
--   - 使用 ddl-auto=update 的环境会自动加列，本脚本用于「手工管理表结构」的生产环境；
--   - 索引请按实际数据量与慢查询情况选择性执行（见文末）。
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. 流程记录表：发起 / 提交 / 当前 / 转交 / 干预 五组操作者的身份
-- -----------------------------------------------------------------------------
ALTER TABLE t_flow_record ADD COLUMN create_operator_identity     VARCHAR(64) NULL;
ALTER TABLE t_flow_record ADD COLUMN submit_operator_identity     VARCHAR(64) NULL;
ALTER TABLE t_flow_record ADD COLUMN current_operator_identity    VARCHAR(64) NULL;
ALTER TABLE t_flow_record ADD COLUMN forward_operator_identity    VARCHAR(64) NULL;
ALTER TABLE t_flow_record ADD COLUMN interfered_operator_identity VARCHAR(64) NULL;


-- -----------------------------------------------------------------------------
-- 2. 待办表：发起 / 提交 / 当前 三组操作者的身份
-- -----------------------------------------------------------------------------
ALTER TABLE t_flow_todo_record ADD COLUMN create_operator_identity  VARCHAR(64) NULL;
ALTER TABLE t_flow_todo_record ADD COLUMN submit_operator_identity  VARCHAR(64) NULL;
ALTER TABLE t_flow_todo_record ADD COLUMN current_operator_identity VARCHAR(64) NULL;


-- -----------------------------------------------------------------------------
-- 2.1 人员分配表：无需改表（issue #288 衍生 / #292）
-- -----------------------------------------------------------------------------
-- 「发起人设定 / 审批人设定」节点的分配关系存在 t_flow_operator_assignment.operator_ids (JSON) 中。
-- 身份编码在该 JSON 内部完成，无需新增列、无需回填：
--   全部默认身份  -> [1,2,3]                                    （保持历史格式）
--   存在指定身份  -> [{"operatorId":2,"identity":"department1"}]
-- 读取端同时兼容两种格式；存量环境不执行本段语句即可正常升级。


-- -----------------------------------------------------------------------------
-- 3. 索引（可选，按需执行）
-- -----------------------------------------------------------------------------
-- 3.1 待办列表查询：where current_operator_id = ? and coalesce(current_operator_identity,'') = ?
--     数据量超过 100 万的待办表建议加此联合索引；小表可暂不加。
--     注意：coalesce 会导致该列无法命中普通索引，因此索引列顺序把 operator_id 放前面，
--     由 operator_id 做范围收敛，身份列做回表过滤。
-- CREATE INDEX idx_todo_operator_identity
--     ON t_flow_todo_record (current_operator_id, current_operator_identity);

-- 3.2 已办 / 抄送列表查询（同上，命中 t_flow_record）
-- CREATE INDEX idx_record_operator_identity
--     ON t_flow_record (current_operator_id, current_operator_identity, record_state);


-- -----------------------------------------------------------------------------
-- 4. 达梦（DM）环境补充说明
-- -----------------------------------------------------------------------------
-- 达梦上 Hibernate 的 ddl-auto=update 对【加列】是可靠的（仅新增 nullable 列，不涉及序列），
-- 但生产环境仍建议按本脚本手工执行。达梦语法与上文一致，字段类型可用 VARCHAR(64)；
-- 若字符集配置为 UTF-8，VARCHAR(64) 即 64 个字符，足以容纳「部门id」「部门id:岗位编码」等复合身份。
-- 达梦不支持 `coalesce` 走索引，索引策略同上（operator_id 在前）。


-- -----------------------------------------------------------------------------
-- 5. 升级后自检（可选）
-- -----------------------------------------------------------------------------
-- 4.1 身份列是否都已创建（不同数据库查询 information_schema 的写法略有差异）
-- SELECT table_name, column_name
--   FROM information_schema.columns
--  WHERE column_name LIKE '%operator_identity'
--  ORDER BY table_name, column_name;

-- 4.2 待办合并键格式抽查：默认身份应为 "id-runtime-node"，非空身份应带 "#identity" 后缀
-- SELECT current_operator_id, current_operator_identity, todo_key
--   FROM t_flow_todo_record
--  ORDER BY id DESC
--  LIMIT 20;
