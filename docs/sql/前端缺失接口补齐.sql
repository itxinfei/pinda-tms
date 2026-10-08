-- 自动生成的补齐脚本：注册缺失资源 + 授权给 role 100
USE pd_auth;

SET @now = NOW();

INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('application_put', '/application', 'PUT', '/application', '前端Application.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('application_post', '/application', 'POST', '/application', '前端Application.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('application_delete', '/application', 'DELETE', '/application', '前端Application.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionary_put', '/dictionary', 'PUT', '/dictionary', '前端Dictionary.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionary_post', '/dictionary', 'POST', '/dictionary', '前端Dictionary.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionary_delete', '/dictionary', 'DELETE', '/dictionary', '前端Dictionary.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionaryItem_put', '/dictionaryItem', 'PUT', '/dictionaryItem', '前端DictionaryItem.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionaryItem_post', '/dictionaryItem', 'POST', '/dictionaryItem', '前端DictionaryItem.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('dictionaryItem_delete', '/dictionaryItem', 'DELETE', '/dictionaryItem', '前端DictionaryItem.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('msgsCenterInfo_page_get', '/msgsCenterInfo/page', 'GET', '/msgsCenterInfo/page', '前端Msgs.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('msgsCenterInfo_mark_get', '/msgsCenterInfo/mark', 'GET', '/msgsCenterInfo/mark', '前端Msgs.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('msgsCenterInfo_get', '/msgsCenterInfo', 'GET', '/msgsCenterInfo', '前端Msgs.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('msgsCenterInfo_post', '/msgsCenterInfo', 'POST', '/msgsCenterInfo', '前端Msgs.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('msgsCenterInfo_get', '/msgsCenterInfo', 'GET', '/msgsCenterInfo', '前端Msgs.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsSendStatus_page_get', '/smsSendStatus/page', 'GET', '/smsSendStatus/page', '前端SmsSendStatus.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTask_page_get', '/smsTask/page', 'GET', '/smsTask/page', '前端SmsTask.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTask_get', '/smsTask', 'GET', '/smsTask', '前端SmsTask.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTask_post', '/smsTask', 'POST', '/smsTask', '前端SmsTask.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTask_put', '/smsTask', 'PUT', '/smsTask', '前端SmsTask.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTask_get', '/smsTask', 'GET', '/smsTask', '前端SmsTask.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTemplate_page_get', '/smsTemplate/page', 'GET', '/smsTemplate/page', '前端SmsTemplate.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTemplate_get', '/smsTemplate', 'GET', '/smsTemplate', '前端SmsTemplate.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTemplate_post', '/smsTemplate', 'POST', '/smsTemplate', '前端SmsTemplate.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTemplate_put', '/smsTemplate', 'PUT', '/smsTemplate', '前端SmsTemplate.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('smsTemplate_check_delete', '/smsTemplate/check', 'DELETE', '/smsTemplate/check', '前端SmsTemplate.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('systemApi_put', '/systemApi', 'PUT', '/systemApi', '前端SystemApi.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('systemApi_post', '/systemApi', 'POST', '/systemApi', '前端SystemApi.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('systemApi_delete', '/systemApi', 'DELETE', '/systemApi', '前端SystemApi.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('tenant_put', '/tenant', 'PUT', '/tenant', '前端Tenant.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('tenant_post', '/tenant', 'POST', '/tenant', '前端Tenant.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('tenant_init_post', '/tenant/init', 'POST', '/tenant/init', '前端Tenant.js', 0, @now);
INSERT INTO pd_auth_resource (code, name, method, url, describe_, is_deleted, create_time) VALUES ('tenant_remove_delete', '/tenant/remove', 'DELETE', '/tenant/remove', '前端Tenant.js', 0, @now);

INSERT INTO pd_auth_role_authority (role_id, authority_id, authority_type) SELECT 100, r.id, 'RESOURCE' FROM pd_auth_resource r LEFT JOIN pd_auth_role_authority ra ON ra.authority_id=r.id AND ra.authority_type='RESOURCE' AND ra.role_id=100 WHERE ra.id IS NULL;