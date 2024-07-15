IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_CHANNEL_DEF_UNIQ') drop index FLW_CHANNEL_DEFINITION.ACT_IDX_CHANNEL_DEF_UNIQ;
IF EXISTS (SELECT name FROM sysindexes WHERE name = 'ACT_IDX_EVENT_DEF_UNIQ') drop index FLW_EVENT_DEFINITION.ACT_IDX_EVENT_DEF_UNIQ;

if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'FLW_CHANNEL_DEFINITION') drop table FLW_CHANNEL_DEFINITION;
if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'FLW_EVENT_DEFINITION') drop table FLW_EVENT_DEFINITION;
if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'FLW_EVENT_RESOURCE') drop table FLW_EVENT_RESOURCE;
if exists (select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'FLW_EVENT_DEPLOYMENT') drop table FLW_EVENT_DEPLOYMENT;