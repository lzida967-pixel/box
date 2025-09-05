// 简单的JWT token刷新功能测试
const { authApi } = require('./src/api/index.ts')

// 模拟token过期场景
async function testTokenRefresh() {
  console.log('测试JWT token刷新功能...')
  
  try {
    // 模拟accessToken过期，调用refreshToken接口
    const refreshToken = 'mock-refresh-token-123'
    const result = await authApi.refreshToken(refreshToken)
    
    console.log('刷新token成功:', result)
    console.log('新的accessToken:', result.data.token)
    console.log('新的refreshToken:', result.data.refreshToken)
    
  } catch (error) {
    console.error('刷新token失败:', error.message)
    // 在实际应用中，这里应该处理token刷新失败的情况
    // 比如跳转到登录页面等
  }
}

// 运行测试
testTokenRefresh()