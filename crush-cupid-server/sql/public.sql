/*
 Navicat Premium Dump SQL

 Source Server         : v而保护
 Source Server Type    : PostgreSQL
 Source Server Version : 180004 (180004)
 Source Host           : localhost:5432
 Source Catalog        : crushCupid
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180004 (180004)
 File Encoding         : 65001

 Date: 04/09/2026 10:43:51
*/


-- ----------------------------
-- Sequence structure for ai_provider_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."ai_provider_id_seq";
CREATE SEQUENCE "public"."ai_provider_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for chat_media_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_media_id_seq";
CREATE SEQUENCE "public"."chat_media_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for chat_source_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chat_source_id_seq";
CREATE SEQUENCE "public"."chat_source_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for conversation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."conversation_id_seq";
CREATE SEQUENCE "public"."conversation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for crush_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."crush_id_seq";
CREATE SEQUENCE "public"."crush_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for crush_report_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."crush_report_id_seq";
CREATE SEQUENCE "public"."crush_report_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for crush_version_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."crush_version_id_seq";
CREATE SEQUENCE "public"."crush_version_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_audit_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_audit_log_id_seq";
CREATE SEQUENCE "public"."sys_audit_log_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_config_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_config_id_seq";
CREATE SEQUENCE "public"."sys_config_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_email_code_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_email_code_id_seq";
CREATE SEQUENCE "public"."sys_email_code_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_perm_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_perm_id_seq";
CREATE SEQUENCE "public"."sys_perm_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_quota_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_quota_id_seq";
CREATE SEQUENCE "public"."sys_quota_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_role_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_role_id_seq";
CREATE SEQUENCE "public"."sys_role_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_usage_daily_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_usage_daily_id_seq";
CREATE SEQUENCE "public"."sys_usage_daily_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sys_user_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."sys_user_id_seq";
CREATE SEQUENCE "public"."sys_user_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for ai_provider
-- ----------------------------
DROP TABLE IF EXISTS "public"."ai_provider";
CREATE TABLE "public"."ai_provider" (
  "id" int8 NOT NULL DEFAULT nextval('ai_provider_id_seq'::regclass),
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "provider_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "base_url" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "api_key" varchar(512) COLLATE "pg_catalog"."default",
  "model" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "temperature" float8 DEFAULT 0.7,
  "top_p" float8,
  "max_tokens" int4,
  "capabilities" varchar(200) COLLATE "pg_catalog"."default" DEFAULT ''::character varying,
  "is_default" bool DEFAULT false,
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now(),
  "user_id" int8,
  "api_key_enc" bytea,
  "api_key_nonce" bytea,
  "api_key_mask" varchar(40) COLLATE "pg_catalog"."default",
  "status" int2 DEFAULT 1,
  "last_used_at" timestamptz(6)
)
;

-- ----------------------------
-- Table structure for chat_media
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_media";
CREATE TABLE "public"."chat_media" (
  "id" int8 NOT NULL DEFAULT nextval('chat_media_id_seq'::regclass),
  "crush_id" int8 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'user'::character varying,
  "media_url" text COLLATE "pg_catalog"."default" NOT NULL,
  "media_type" varchar(50) COLLATE "pg_catalog"."default" DEFAULT 'image'::character varying,
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for chat_source
-- ----------------------------
DROP TABLE IF EXISTS "public"."chat_source";
CREATE TABLE "public"."chat_source" (
  "id" int8 NOT NULL DEFAULT nextval('chat_source_id_seq'::regclass),
  "crush_id" int8 NOT NULL,
  "file_name" varchar(255) COLLATE "pg_catalog"."default",
  "file_path" varchar(500) COLLATE "pg_catalog"."default",
  "file_type" varchar(50) COLLATE "pg_catalog"."default",
  "file_format" varchar(20) COLLATE "pg_catalog"."default",
  "message_count" int4 DEFAULT 0,
  "raw_analysis" jsonb,
  "parsed_at" timestamptz(6),
  "created_at" timestamptz(6) DEFAULT now(),
  "content" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS "public"."conversation";
CREATE TABLE "public"."conversation" (
  "id" int8 NOT NULL DEFAULT nextval('conversation_id_seq'::regclass),
  "crush_id" int8 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) DEFAULT now(),
  "user_id" int8 DEFAULT 0
)
;

-- ----------------------------
-- Table structure for crush
-- ----------------------------
DROP TABLE IF EXISTS "public"."crush";
CREATE TABLE "public"."crush" (
  "id" int8 NOT NULL DEFAULT nextval('crush_id_seq'::regclass),
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "slug" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "mbti" varchar(10) COLLATE "pg_catalog"."default",
  "zodiac" varchar(20) COLLATE "pg_catalog"."default",
  "occupation" varchar(100) COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "know_duration" varchar(50) COLLATE "pg_catalog"."default",
  "relationship_status" varchar(50) COLLATE "pg_catalog"."default",
  "impression" text COLLATE "pg_catalog"."default",
  "persona_layer0" text COLLATE "pg_catalog"."default",
  "persona_layer1" text COLLATE "pg_catalog"."default",
  "persona_layer2" text COLLATE "pg_catalog"."default",
  "persona_layer3" text COLLATE "pg_catalog"."default",
  "persona_layer4" text COLLATE "pg_catalog"."default",
  "memory_overview" text COLLATE "pg_catalog"."default",
  "memory_timeline" jsonb,
  "memory_sweet" text COLLATE "pg_catalog"."default",
  "memory_interaction" text COLLATE "pg_catalog"."default",
  "current_stage" int2 DEFAULT 1,
  "total_messages" int4 DEFAULT 0,
  "last_chat_date" timestamptz(6),
  "version" int4 DEFAULT 1,
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now(),
  "status" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'DRAFT'::character varying,
  "voice_id" varchar(100) COLLATE "pg_catalog"."default",
  "proactive_enabled" bool DEFAULT true,
  "next_proactive_at" timestamptz(6),
  "last_proactive_at" timestamptz(6),
  "proactive_date" date,
  "proactive_count" int4 DEFAULT 0,
  "user_id" int8 DEFAULT 0
)
;

-- ----------------------------
-- Table structure for crush_report
-- ----------------------------
DROP TABLE IF EXISTS "public"."crush_report";
CREATE TABLE "public"."crush_report" (
  "id" int8 NOT NULL DEFAULT nextval('crush_report_id_seq'::regclass),
  "crush_id" int8 NOT NULL,
  "crush_name" varchar(100) COLLATE "pg_catalog"."default",
  "title" varchar(255) COLLATE "pg_catalog"."default",
  "markdown" text COLLATE "pg_catalog"."default" NOT NULL,
  "source" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'manual'::character varying,
  "report_date" date,
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for crush_version
-- ----------------------------
DROP TABLE IF EXISTS "public"."crush_version";
CREATE TABLE "public"."crush_version" (
  "id" int8 NOT NULL DEFAULT nextval('crush_version_id_seq'::regclass),
  "crush_id" int8 NOT NULL,
  "version" int4 NOT NULL,
  "snapshot" jsonb NOT NULL,
  "reason" varchar(500) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_audit_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_audit_log";
CREATE TABLE "public"."sys_audit_log" (
  "id" int8 NOT NULL DEFAULT nextval('sys_audit_log_id_seq'::regclass),
  "user_id" int8,
  "email" varchar(120) COLLATE "pg_catalog"."default",
  "module" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "action" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "resource_type" varchar(50) COLLATE "pg_catalog"."default",
  "resource_id" varchar(64) COLLATE "pg_catalog"."default",
  "detail" jsonb,
  "request_id" varchar(64) COLLATE "pg_catalog"."default",
  "ip" varchar(46) COLLATE "pg_catalog"."default",
  "user_agent" varchar(255) COLLATE "pg_catalog"."default",
  "result" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "error_message" varchar(500) COLLATE "pg_catalog"."default",
  "latency_ms" int4,
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_config";
CREATE TABLE "public"."sys_config" (
  "id" int8 NOT NULL DEFAULT nextval('sys_config_id_seq'::regclass),
  "config_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "config_value" text COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "updated_by" int8,
  "updated_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_email_code
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_email_code";
CREATE TABLE "public"."sys_email_code" (
  "id" int8 NOT NULL DEFAULT nextval('sys_email_code_id_seq'::regclass),
  "email" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "purpose" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "code_hash" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "expire_at" timestamptz(6) NOT NULL,
  "used" bool DEFAULT false,
  "attempt" int4 DEFAULT 0,
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_perm
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_perm";
CREATE TABLE "public"."sys_perm" (
  "id" int8 NOT NULL DEFAULT nextval('sys_perm_id_seq'::regclass),
  "code" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default",
  "type" varchar(20) COLLATE "pg_catalog"."default" DEFAULT 'API'::character varying,
  "module" varchar(50) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_quota
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_quota";
CREATE TABLE "public"."sys_quota" (
  "id" int8 NOT NULL DEFAULT nextval('sys_quota_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "plan" varchar(30) COLLATE "pg_catalog"."default" DEFAULT 'free'::character varying,
  "crush_limit" int4,
  "daily_chat_limit" int4,
  "model_allowlist" jsonb,
  "effective_from" timestamptz(6),
  "effective_to" timestamptz(6),
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role";
CREATE TABLE "public"."sys_role" (
  "id" int8 NOT NULL DEFAULT nextval('sys_role_id_seq'::regclass),
  "code" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "builtin" bool DEFAULT false,
  "created_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_role_perm
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_role_perm";
CREATE TABLE "public"."sys_role_perm" (
  "role_id" int8 NOT NULL,
  "perm_id" int8 NOT NULL
)
;

-- ----------------------------
-- Table structure for sys_usage_daily
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_usage_daily";
CREATE TABLE "public"."sys_usage_daily" (
  "id" int8 NOT NULL DEFAULT nextval('sys_usage_daily_id_seq'::regclass),
  "user_id" int8 NOT NULL,
  "usage_date" date NOT NULL,
  "chat_count" int4 DEFAULT 0,
  "message_count" int4 DEFAULT 0,
  "provider_calls" int4 DEFAULT 0,
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user";
CREATE TABLE "public"."sys_user" (
  "id" int8 NOT NULL DEFAULT nextval('sys_user_id_seq'::regclass),
  "email" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "username" varchar(50) COLLATE "pg_catalog"."default",
  "password_hash" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "avatar_url" varchar(500) COLLATE "pg_catalog"."default",
  "status" int2 NOT NULL DEFAULT 1,
  "email_verified" bool NOT NULL DEFAULT false,
  "failed_attempt" int4 NOT NULL DEFAULT 0,
  "locked_until" timestamptz(6),
  "last_login_at" timestamptz(6),
  "last_login_ip" varchar(46) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sys_user_key
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_key";
CREATE TABLE "public"."sys_user_key" (
  "user_id" int8 NOT NULL,
  "key_enc" bytea NOT NULL,
  "key_nonce" bytea NOT NULL,
  "created_at" timestamptz(6) DEFAULT now(),
  "updated_at" timestamptz(6) DEFAULT now()
)
;
COMMENT ON TABLE "public"."sys_user_key" IS '聊天记录点对点加密：每用户独立 AES 密钥（全局 KEK 包裹存储）';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."sys_user_role";
CREATE TABLE "public"."sys_user_role" (
  "user_id" int8 NOT NULL,
  "role_id" int8 NOT NULL
)
;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."ai_provider_id_seq"
OWNED BY "public"."ai_provider"."id";
SELECT setval('"public"."ai_provider_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_media_id_seq"
OWNED BY "public"."chat_media"."id";
SELECT setval('"public"."chat_media_id_seq"', 7, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chat_source_id_seq"
OWNED BY "public"."chat_source"."id";
SELECT setval('"public"."chat_source_id_seq"', 8, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."conversation_id_seq"
OWNED BY "public"."conversation"."id";
SELECT setval('"public"."conversation_id_seq"', 16558, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."crush_id_seq"
OWNED BY "public"."crush"."id";
SELECT setval('"public"."crush_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."crush_report_id_seq"
OWNED BY "public"."crush_report"."id";
SELECT setval('"public"."crush_report_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."crush_version_id_seq"
OWNED BY "public"."crush_version"."id";
SELECT setval('"public"."crush_version_id_seq"', 7, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_audit_log_id_seq"
OWNED BY "public"."sys_audit_log"."id";
SELECT setval('"public"."sys_audit_log_id_seq"', 8, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_config_id_seq"
OWNED BY "public"."sys_config"."id";
SELECT setval('"public"."sys_config_id_seq"', 5, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_email_code_id_seq"
OWNED BY "public"."sys_email_code"."id";
SELECT setval('"public"."sys_email_code_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_perm_id_seq"
OWNED BY "public"."sys_perm"."id";
SELECT setval('"public"."sys_perm_id_seq"', 28, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_quota_id_seq"
OWNED BY "public"."sys_quota"."id";
SELECT setval('"public"."sys_quota_id_seq"', 1, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_role_id_seq"
OWNED BY "public"."sys_role"."id";
SELECT setval('"public"."sys_role_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_usage_daily_id_seq"
OWNED BY "public"."sys_usage_daily"."id";
SELECT setval('"public"."sys_usage_daily_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."sys_user_id_seq"
OWNED BY "public"."sys_user"."id";
SELECT setval('"public"."sys_user_id_seq"', 1, true);

-- ----------------------------
-- Indexes structure for table ai_provider
-- ----------------------------
CREATE INDEX "idx_ai_provider_user" ON "public"."ai_provider" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE UNIQUE INDEX "idx_ai_provider_user_key" ON "public"."ai_provider" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "provider_key" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
) WHERE user_id IS NOT NULL;

-- ----------------------------
-- Primary Key structure for table ai_provider
-- ----------------------------
ALTER TABLE "public"."ai_provider" ADD CONSTRAINT "ai_provider_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table chat_media
-- ----------------------------
CREATE INDEX "idx_chat_media_crush" ON "public"."chat_media" USING btree (
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table chat_media
-- ----------------------------
ALTER TABLE "public"."chat_media" ADD CONSTRAINT "chat_media_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table chat_source
-- ----------------------------
CREATE INDEX "idx_chat_source_crush" ON "public"."chat_source" USING btree (
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table chat_source
-- ----------------------------
ALTER TABLE "public"."chat_source" ADD CONSTRAINT "chat_source_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table conversation
-- ----------------------------
CREATE INDEX "idx_conversation_crush" ON "public"."conversation" USING btree (
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
);
CREATE INDEX "idx_conversation_user_crush" ON "public"."conversation" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table conversation
-- ----------------------------
ALTER TABLE "public"."conversation" ADD CONSTRAINT "conversation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crush
-- ----------------------------
CREATE INDEX "idx_crush_user" ON "public"."crush" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table crush
-- ----------------------------
ALTER TABLE "public"."crush" ADD CONSTRAINT "crush_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Primary Key structure for table crush
-- ----------------------------
ALTER TABLE "public"."crush" ADD CONSTRAINT "crush_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crush_report
-- ----------------------------
CREATE INDEX "idx_crush_report_crush" ON "public"."crush_report" USING btree (
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "report_date" "pg_catalog"."date_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table crush_report
-- ----------------------------
ALTER TABLE "public"."crush_report" ADD CONSTRAINT "crush_report_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table crush_version
-- ----------------------------
CREATE INDEX "idx_version_crush" ON "public"."crush_version" USING btree (
  "crush_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "version" "pg_catalog"."int4_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table crush_version
-- ----------------------------
ALTER TABLE "public"."crush_version" ADD CONSTRAINT "crush_version_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_audit_log
-- ----------------------------
CREATE INDEX "idx_audit_module" ON "public"."sys_audit_log" USING btree (
  "module" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_audit_resource" ON "public"."sys_audit_log" USING btree (
  "resource_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "resource_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_audit_user" ON "public"."sys_audit_log" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table sys_audit_log
-- ----------------------------
ALTER TABLE "public"."sys_audit_log" ADD CONSTRAINT "sys_audit_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_config
-- ----------------------------
ALTER TABLE "public"."sys_config" ADD CONSTRAINT "sys_config_config_key_key" UNIQUE ("config_key");

-- ----------------------------
-- Primary Key structure for table sys_config
-- ----------------------------
ALTER TABLE "public"."sys_config" ADD CONSTRAINT "sys_config_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_email_code
-- ----------------------------
CREATE INDEX "idx_email_code" ON "public"."sys_email_code" USING btree (
  "email" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "purpose" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table sys_email_code
-- ----------------------------
ALTER TABLE "public"."sys_email_code" ADD CONSTRAINT "sys_email_code_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_perm
-- ----------------------------
ALTER TABLE "public"."sys_perm" ADD CONSTRAINT "sys_perm_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table sys_perm
-- ----------------------------
ALTER TABLE "public"."sys_perm" ADD CONSTRAINT "sys_perm_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_quota
-- ----------------------------
CREATE UNIQUE INDEX "idx_quota_user" ON "public"."sys_quota" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_quota
-- ----------------------------
ALTER TABLE "public"."sys_quota" ADD CONSTRAINT "sys_quota_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table sys_role
-- ----------------------------
ALTER TABLE "public"."sys_role" ADD CONSTRAINT "sys_role_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_role_perm
-- ----------------------------
ALTER TABLE "public"."sys_role_perm" ADD CONSTRAINT "sys_role_perm_pkey" PRIMARY KEY ("role_id", "perm_id");

-- ----------------------------
-- Indexes structure for table sys_usage_daily
-- ----------------------------
CREATE INDEX "idx_usage_user" ON "public"."sys_usage_daily" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "usage_date" "pg_catalog"."date_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Uniques structure for table sys_usage_daily
-- ----------------------------
ALTER TABLE "public"."sys_usage_daily" ADD CONSTRAINT "sys_usage_daily_user_id_usage_date_key" UNIQUE ("user_id", "usage_date");

-- ----------------------------
-- Primary Key structure for table sys_usage_daily
-- ----------------------------
ALTER TABLE "public"."sys_usage_daily" ADD CONSTRAINT "sys_usage_daily_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sys_user
-- ----------------------------
CREATE UNIQUE INDEX "idx_sys_user_email" ON "public"."sys_user" USING btree (
  lower(email::text) COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_user
-- ----------------------------
ALTER TABLE "public"."sys_user" ADD CONSTRAINT "sys_user_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sys_user_key
-- ----------------------------
ALTER TABLE "public"."sys_user_key" ADD CONSTRAINT "sys_user_key_pkey" PRIMARY KEY ("user_id");

-- ----------------------------
-- Indexes structure for table sys_user_role
-- ----------------------------
CREATE INDEX "idx_user_role_user" ON "public"."sys_user_role" USING btree (
  "user_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_pkey" PRIMARY KEY ("user_id", "role_id");

-- ----------------------------
-- Foreign Keys structure for table chat_media
-- ----------------------------
ALTER TABLE "public"."chat_media" ADD CONSTRAINT "chat_media_crush_id_fkey" FOREIGN KEY ("crush_id") REFERENCES "public"."crush" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table chat_source
-- ----------------------------
ALTER TABLE "public"."chat_source" ADD CONSTRAINT "chat_source_crush_id_fkey" FOREIGN KEY ("crush_id") REFERENCES "public"."crush" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table conversation
-- ----------------------------
ALTER TABLE "public"."conversation" ADD CONSTRAINT "conversation_crush_id_fkey" FOREIGN KEY ("crush_id") REFERENCES "public"."crush" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table crush_report
-- ----------------------------
ALTER TABLE "public"."crush_report" ADD CONSTRAINT "crush_report_crush_id_fkey" FOREIGN KEY ("crush_id") REFERENCES "public"."crush" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table crush_version
-- ----------------------------
ALTER TABLE "public"."crush_version" ADD CONSTRAINT "crush_version_crush_id_fkey" FOREIGN KEY ("crush_id") REFERENCES "public"."crush" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_quota
-- ----------------------------
ALTER TABLE "public"."sys_quota" ADD CONSTRAINT "sys_quota_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."sys_user" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_role_perm
-- ----------------------------
ALTER TABLE "public"."sys_role_perm" ADD CONSTRAINT "sys_role_perm_perm_id_fkey" FOREIGN KEY ("perm_id") REFERENCES "public"."sys_perm" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_role_perm" ADD CONSTRAINT "sys_role_perm_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "public"."sys_role" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_usage_daily
-- ----------------------------
ALTER TABLE "public"."sys_usage_daily" ADD CONSTRAINT "sys_usage_daily_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."sys_user" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sys_user_role
-- ----------------------------
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_role_id_fkey" FOREIGN KEY ("role_id") REFERENCES "public"."sys_role" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."sys_user_role" ADD CONSTRAINT "sys_user_role_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."sys_user" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
