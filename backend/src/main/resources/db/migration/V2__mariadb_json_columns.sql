-- MariaDB maps JSON columns to LONGTEXT; align schema with entity mappings.
ALTER TABLE projects MODIFY tech_stack LONGTEXT NULL;
ALTER TABLE projects MODIFY architecture LONGTEXT NULL;
ALTER TABLE projects MODIFY requirements LONGTEXT NULL;
ALTER TABLE projects MODIFY file_tree LONGTEXT NULL;
ALTER TABLE projects MODIFY dev_history LONGTEXT NULL;
ALTER TABLE project_files MODIFY language VARCHAR(50) NULL;
ALTER TABLE ai_messages MODIFY tool_calls LONGTEXT NULL;
ALTER TABLE ai_actions MODIFY payload LONGTEXT NULL;
ALTER TABLE build_runs MODIFY errors LONGTEXT NULL;
ALTER TABLE test_runs MODIFY results LONGTEXT NULL;
ALTER TABLE project_versions MODIFY details LONGTEXT NULL;
ALTER TABLE audit_logs MODIFY details LONGTEXT NULL;
