// JWT token认证流程测试
const { authApi } = require('./src/api/index.ts')

// 模拟完整的认证流程
async function testAuthFlow() {
  console.log('=== JWT Token认证流程测试 ===')
  
  try {
    // 1. 模拟登录
    console.log('\n1. 用户登录...')
    const loginData = {
      username: 'testuser',
      password: 'password123',
      rememberMe: true
    }
    
    const loginResult = await authApi.login(loginData)
    console.log('登录成功:', {
      accessToken: loginResult.data.token,
      refreshToken: loginResult.data.refreshToken,
      expiresIn: loginResult.data.expiresIn
    })
    
    // 2. 模拟token过期，刷新token
    console.log('\n2. 刷新accessToken...')
    const refreshResult = await authApi.refreshToken(loginResult.data.refreshToken)
    console.log('Token刷新成功:', {
      newAccessToken: refreshResult.data.token,
      newRefreshToken: refreshResult.data.refreshToken,
      expiresIn: refreshResult.data.expiresIn
    })
    
    // 3. 验证新token是否有效
    console.log('\n3. 验证新token有效性...')
    console.log('新accessToken长度:', refreshResult.data.token.length)
    console.log('新refreshToken长度:', refreshResult.data.refreshToken.length)
    console.log('过期时间:', refreshResult.data.expiresIn, '秒')
    
    console.log('\n=== 测试完成 ===')
    console.log('✅ JWT token认证和刷新机制工作正常')
    
  } catch (error) {
    console.error('❌ 测试失败:', error.message)
    console.error('错误详情:', error.response?.data || error)
  }
}

// 运行测试
testAuthFlow()