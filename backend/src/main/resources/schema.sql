`users``users`-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    PASSWORD VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    email VARCHAR(100) NULL COMMENT '邮箱',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    STATUS INT DEFAULT 0 COMMENT '在线状态: 0-离线, 1-在线, 2-忙碌, 3-离开',
    signature VARCHAR(200) COMMENT '个性签名',
    phone VARCHAR(20) COMMENT '手机号',
    gender INT DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    birthday TIMESTAMP NULL COMMENT '生日',
    last_login_time TIMESTAMP NULL COMMENT '最后登录时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (STATUS),
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建群组表
CREATE TABLE IF NOT EXISTS `groups` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '群组ID',
    NAME VARCHAR(100) NOT NULL COMMENT '群组名称',
    DESCRIPTION VARCHAR(500) COMMENT '群组描述',
    avatar VARCHAR(500) COMMENT '群组头像URL',
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    member_count INT DEFAULT 0 COMMENT '当前成员数',
    STATUS INT DEFAULT 1 COMMENT '群组状态: 0-禁用, 1-正常',
    invite_code VARCHAR(50) COMMENT '邀请码',
    allow_invite INT DEFAULT 1 COMMENT '是否允许邀请: 0-不允许, 1-允许',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    INDEX idx_owner_id (owner_id),
    INDEX idx_status (STATUS),
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 创建消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    from_user_id BIGINT NOT NULL COMMENT '发送者ID',
    to_user_id BIGINT COMMENT '接收者ID（私聊消息）',
    group_id BIGINT COMMENT '群组ID（群聊消息）',
    message_type INT DEFAULT 1 COMMENT '消息类型: 1-文本, 2-图片, 3-文件, 4-语音, 5-视频, 6-系统消息',
    content TEXT NOT NULL COMMENT '消息内容',
    file_url VARCHAR(500) COMMENT '文件URL（文件消息）',
    file_name VARCHAR(255) COMMENT '文件名（文件消息）',
    file_size BIGINT COMMENT '文件大小（文件消息）',
    STATUS INT DEFAULT 0 COMMENT '消息状态: 0-未读, 1-已读, 2-已撤回',
    reply_to_id BIGINT COMMENT '回复的消息ID',
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    read_time TIMESTAMP NULL COMMENT '阅读时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    INDEX idx_from_user_id (from_user_id),
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_message_type (message_type),
    INDEX idx_status (STATUS),
    INDEX idx_send_time (send_time),
    INDEX idx_reply_to_id (reply_to_id),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 创建群组成员表
CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成员关系ID',
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    ROLE INT DEFAULT 0 COMMENT '角色: 0-普通成员, 1-管理员, 2-群主',
    nickname VARCHAR(100) COMMENT '群内昵称',
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    mute_until TIMESTAMP NULL COMMENT '禁言截止时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (ROLE),
    INDEX idx_join_time (join_time),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 创建好友关系表
CREATE TABLE IF NOT EXISTS friendships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '好友关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    STATUS INT DEFAULT 0 COMMENT '关系状态: 0-待确认, 1-已确认, 2-已拉黑',
    nickname VARCHAR(100) COMMENT '好友备注昵称',
    group_name VARCHAR(100) COMMENT '好友分组',
    request_message VARCHAR(200) COMMENT '好友申请消息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id),
    INDEX idx_status (STATUS),
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 创建会话表
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_id BIGINT NOT NULL COMMENT '对话目标ID（用户ID或群组ID）',
    conversation_type INT DEFAULT 1 COMMENT '会话类型: 1-私聊, 2-群聊',
    last_message_id BIGINT COMMENT '最后一条消息ID',
    last_message_time TIMESTAMP NULL COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    is_top INT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    is_mute INT DEFAULT 0 COMMENT '是否静音: 0-否, 1-是',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',

    UNIQUE KEY uk_user_target_type (user_id, target_id, conversation_type),
    INDEX idx_user_id (user_id),
    INDEX idx_target_id (target_id),
    INDEX idx_conversation_type (conversation_type),
    INDEX idx_last_message_time (last_message_time),
    INDEX idx_last_message_id (last_message_id),
    INDEX idx_is_top (is_top),
    INDEX idx_deleted (deleted)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NULL;

ALTER TABLE users ADD COLUMN avatar_data LONGBLOB COMMENT '头像二进制数据';
ALTER TABLE users ADD COLUMN avatar_content_type VARCHAR(100) COMMENT '头像文件类型';