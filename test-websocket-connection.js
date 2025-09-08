// WebSocket连接测试脚本
// 在Node.js环境中运行: node test-websocket-connection.js

const WebSocket = require('ws');

// 测试token (从浏览器localStorage中获取)
const testToken = 'YWFhOjk4YTg4NmRlLTkyZTktNDFkZC1iMmY0LTBmNWM5MjNiNjdkYjoxNzU3MzE1Nzg4NDAw';

console.log('🔍 开始WebSocket连接测试...');
console.log('Token:', testToken);

// 解码token查看内容
try {
    const decoded = Buffer.from(testToken, 'base64').toString('utf-8');
    console.log('解码后的token:', decoded);
    
    const parts = decoded.split(':');
    console.log('Token部分:');
    console.log('  用户名:', parts[0]);
    console.log('  UUID:', parts[1]);
    console.log('  时间戳:', parts[2]);
    
    if (parts[2]) {
        const timestamp = parseInt(parts[2]);
        const date = new Date(timestamp);
        console.log('  创建时间:', date.toLocaleString());
        
        const now = Date.now();
        const age = now - timestamp;
        const ageHours = age / (1000 * 60 * 60);
        console.log('  Token年龄:', ageHours.toFixed(2), '小时');
        
        if (ageHours > 24) {
            console.log('⚠️  Token可能已过期 (超过24小时)');
        }
    }
} catch (e) {
    console.error('❌ Token解码失败:', e.message);
}

console.log('\n🔗 尝试连接WebSocket...');

const wsUrl = `ws://localhost:8080/api/ws/chat?token=${testToken}`;
console.log('连接URL:', wsUrl);

const ws = new WebSocket(wsUrl);

ws.on('open', function open() {
    console.log('✅ WebSocket连接成功!');
    
    // 发送测试消息
    const testMessage = {
        type: 'CHAT_MESSAGE',
        content: '测试消息',
        receiverId: 2,
        timestamp: Date.now()
    };
    
    console.log('📤 发送测试消息:', testMessage);
    ws.send(JSON.stringify(testMessage));
    
    // 5秒后关闭连接
    setTimeout(() => {
        console.log('🔚 关闭连接');
        ws.close();
    }, 5000);
});

ws.on('message', function message(data) {
    console.log('📨 收到消息:', data.toString());
});

ws.on('error', function error(err) {
    console.error('❌ WebSocket连接错误:', err.message);
    console.error('错误详情:', err);
});

ws.on('close', function close(code, reason) {
    console.log('🔚 WebSocket连接关闭');
    console.log('关闭代码:', code);
    console.log('关闭原因:', reason.toString());
    
    // 解释关闭代码
    const closeCodes = {
        1000: '正常关闭',
        1001: '端点离开',
        1002: '协议错误',
        1003: '不支持的数据类型',
        1006: '异常关闭',
        1007: '数据格式错误',
        1008: '策略违规',
        1009: '消息过大',
        1010: '扩展协商失败',
        1011: '服务器错误'
    };
    
    console.log('关闭说明:', closeCodes[code] || '未知错误');
});

// 超时处理
setTimeout(() => {
    if (ws.readyState === WebSocket.CONNECTING) {
        console.log('⏰ 连接超时，强制关闭');
        ws.terminate();
    }
}, 10000);