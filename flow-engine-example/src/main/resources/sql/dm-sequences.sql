-- =====================================================================
-- 达梦( DM ) 主键序列预建脚本
-- =====================================================================
-- 适用表( SEQUENCE 主键，配合 hibernate 批插 ):
--   t_flow_record            -> t_flow_record_seq
--   t_flow_todo_record       -> t_flow_todo_record_seq
--   t_flow_todo_marge        -> t_flow_todo_marge_seq
--   t_flow_sub_process_record-> t_flow_sub_process_record_seq
--
-- 序列名 / 步长 与实体 @SequenceGenerator(sequenceName=..., allocationSize=50) 严格一致，
-- 请勿修改，否则 hibernate 会在 insert 时报“无法解析的成员访问表达式[...NEXTVAL]”或出现主键重复。
--
-- 特性:
--   * INCREMENT BY 固定 50（必须 == allocationSize，paired/pooled 优化器要求，否则可能主键重复）
--   * START WITH 自动取 「表 max(id) + 1」：表为空则 1，表有历史数据则最大 id+1（绝不撞已有主键）
--   * 幂等：序列已存在则跳过，可反复执行
--   * 基于当前登录用户(USER)所在模式，与表和序列归属一致
--
-- 执行方式：DBA 工具(达梦管理工具/disql) 以应用同款账号(默认 SYSDBA) 执行本脚本即可。
-- =====================================================================

DECLARE
    v_cnt   INT;
    v_start BIGINT;
BEGIN
    -- ---------- 1. t_flow_record_seq ----------
    SELECT COUNT(*) INTO v_cnt FROM all_sequences
     WHERE sequence_name = 'T_FLOW_RECORD_SEQ' AND sequence_owner = USER;
    IF v_cnt = 0 THEN
        BEGIN
            SELECT NVL(MAX(id), 0) + 1 INTO v_start FROM t_flow_record;
        EXCEPTION WHEN OTHERS THEN
            v_start := 1;   -- 表不存在或为空
        END;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE t_flow_record_seq INCREMENT BY 50 START WITH ' || v_start;
    END IF;

    -- ---------- 2. t_flow_todo_record_seq ----------
    SELECT COUNT(*) INTO v_cnt FROM all_sequences
     WHERE sequence_name = 'T_FLOW_TODO_RECORD_SEQ' AND sequence_owner = USER;
    IF v_cnt = 0 THEN
        BEGIN
            SELECT NVL(MAX(id), 0) + 1 INTO v_start FROM t_flow_todo_record;
        EXCEPTION WHEN OTHERS THEN
            v_start := 1;
        END;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE t_flow_todo_record_seq INCREMENT BY 50 START WITH ' || v_start;
    END IF;

    -- ---------- 3. t_flow_todo_marge_seq ----------
    SELECT COUNT(*) INTO v_cnt FROM all_sequences
     WHERE sequence_name = 'T_FLOW_TODO_MARGE_SEQ' AND sequence_owner = USER;
    IF v_cnt = 0 THEN
        BEGIN
            SELECT NVL(MAX(id), 0) + 1 INTO v_start FROM t_flow_todo_marge;
        EXCEPTION WHEN OTHERS THEN
            v_start := 1;
        END;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE t_flow_todo_marge_seq INCREMENT BY 50 START WITH ' || v_start;
    END IF;

    -- ---------- 4. t_flow_sub_process_record_seq ----------
    SELECT COUNT(*) INTO v_cnt FROM all_sequences
     WHERE sequence_name = 'T_FLOW_SUB_PROCESS_RECORD_SEQ' AND sequence_owner = USER;
    IF v_cnt = 0 THEN
        BEGIN
            SELECT NVL(MAX(id), 0) + 1 INTO v_start FROM t_flow_sub_process_record;
        EXCEPTION WHEN OTHERS THEN
            v_start := 1;
        END;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE t_flow_sub_process_record_seq INCREMENT BY 50 START WITH ' || v_start;
    END IF;
END;
/