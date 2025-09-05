-- 测试用户数据
INSERT INTO users (username, password, email, nickname, signature, gender, status) VALUES 
('aaa', '$2a$10$x8nwHVy5Y8DZs5B7qZ5ZK.7j7vQ4LHXHGfY0sQ1L8YP9C.S8Dy4xq', 'aaa@example.com', '测试用户A', '这是我的个性签名', 1, 1),
('bbb', '$2a$10$x8nwHVy5Y8DZs5B7qZ5ZK.7j7vQ4LHXHGfY0sQ1L8YP9C.S8Dy4xq', 'bbb@example.com', '测试用户B', '另一个个性签名', 2, 1),
('ccc', '$2a$10$x8nwHVy5Y8DZs5B7qZ5ZK.7j7vQ4LHXHGfY0sQ1L8YP9C.S8Dy4xq', 'ccc@example.com', '测试用户C', '', 1, 0)
ON DUPLICATE KEY UPDATE 
    nickname = VALUES(nickname),
    signature = VALUES(signature),
    gender = VALUES(gender),
    status = VALUES(status);