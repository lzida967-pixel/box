/**
 * CORS测试工具
 * 用于测试跨域请求是否正常工作
 */

const API_BASE_URL = 'http://localhost:8080/api'

export class CorsTest {
    /**
     * 测试OPTIONS预检请求
     */
    static async testPreflight(url: string): Promise<void> {
        console.log('=== CORS预检请求测试 ===')
        console.log('测试URL:', url)

        try {
            const response = await fetch(url, {
                method: 'OPTIONS',
                headers: {
                    'Origin': 'http://localhost:3000',
                    'Access-Control-Request-Method': 'GET',
                    'Access-Control-Request-Headers': 'authorization,content-type'
                }
            })

            console.log('预检请求状态:', response.status)
            console.log('预检请求响应头:')
            response.headers.forEach((value, key) => {
                if (key.startsWith('access-control-')) {
                    console.log(`  ${key}: ${value}`)
                }
            })

            if (response.ok) {
                console.log('✅ 预检请求成功')
            } else {
                console.log('❌ 预检请求失败')
            }
        } catch (error) {
            console.error('❌ 预检请求错误:', error)
        }
    }

    /**
     * 测试GET请求
     */
    static async testGet(url: string, params?: Record<string, string>): Promise<void> {
        console.log('=== GET请求测试 ===')

        let testUrl = url
        if (params) {
            const searchParams = new URLSearchParams(params)
            testUrl += '?' + searchParams.toString()
        }

        console.log('测试URL:', testUrl)

        try {
            const response = await fetch(testUrl, {
                method: 'GET',
                headers: {
                    'Origin': 'http://localhost:3000',
                    'Accept': 'application/json'
                }
            })

            console.log('GET请求状态:', response.status)
            console.log('GET请求响应头:')
            response.headers.forEach((value, key) => {
                if (key.startsWith('access-control-') || key === 'content-type') {
                    console.log(`  ${key}: ${value}`)
                }
            })

            if (response.ok) {
                const data = await response.text()
                console.log('✅ GET请求成功')
                console.log('响应数据:', data.substring(0, 200) + (data.length > 200 ? '...' : ''))
            } else {
                console.log('❌ GET请求失败')
                const errorText = await response.text()
                console.log('错误响应:', errorText.substring(0, 200))
            }
        } catch (error) {
            console.error('❌ GET请求错误:', error)
        }
    }

    /**
     * 测试带认证的请求
     */
    static async testWithAuth(url: string, token: string): Promise<void> {
        console.log('=== 带认证的请求测试 ===')
        console.log('测试URL:', url)
        console.log('Token:', token.substring(0, 20) + '...')

        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Origin': 'http://localhost:3000',
                    'Accept': 'application/json',
                    'Authorization': `Bearer ${token}`
                }
            })

            console.log('认证请求状态:', response.status)
            console.log('认证请求响应头:')
            response.headers.forEach((value, key) => {
                if (key.startsWith('access-control-') || key === 'content-type') {
                    console.log(`  ${key}: ${value}`)
                }
            })

            if (response.ok) {
                const data = await response.text()
                console.log('✅ 认证请求成功')
                console.log('响应数据:', data.substring(0, 200) + (data.length > 200 ? '...' : ''))
            } else {
                console.log('❌ 认证请求失败')
                const errorText = await response.text()
                console.log('错误响应:', errorText.substring(0, 200))
            }
        } catch (error) {
            console.error('❌ 认证请求错误:', error)
        }
    }

    /**
     * 检查服务器状态
     */
    static async checkServerStatus(): Promise<void> {
        console.log('=== 服务器状态检查 ===')

        try {
            const response = await fetch(`${API_BASE_URL}/test/hello`, {
                method: 'GET',
                headers: {
                    'Origin': 'http://localhost:3000'
                }
            })

            console.log('服务器状态:', response.status)

            if (response.ok) {
                console.log('✅ 服务器运行正常')
            } else {
                console.log('❌ 服务器响应异常')
            }
        } catch (error) {
            console.error('❌ 无法连接到服务器:', error)
        }
    }

    /**
     * 运行完整的CORS测试
     */
    static async runFullTest(): Promise<void> {
        console.log('🚀 开始CORS完整测试...')
        console.log('目标服务器:', API_BASE_URL)
        console.log('请求源:', 'http://localhost:3000')
        console.log('')

        // 1. 检查服务器状态
        await this.checkServerStatus()
        console.log('')

        // 2. 测试预检请求
        await this.testPreflight(`${API_BASE_URL}/contacts/search`)
        console.log('')

        // 3. 测试GET请求
        await this.testGet(`${API_BASE_URL}/contacts/search`, { keyword: 'test' })
        console.log('')

        // 4. 测试认证请求（如果有token）
        const authUser = localStorage.getItem('authUser')
        if (authUser) {
            try {
                const user = JSON.parse(authUser)
                if (user.token) {
                    await this.testWithAuth(`${API_BASE_URL}/contacts/search?keyword=test`, user.token)
                }
            } catch (e) {
                console.log('无法解析认证信息，跳过认证测试')
            }
        } else {
            console.log('未找到认证信息，跳过认证测试')
        }

        console.log('')
        console.log('🏁 CORS测试完成')
    }
}

// 在控制台中提供全局访问
if (typeof window !== 'undefined') {
    (window as any).corsTest = CorsTest
    console.log('CORS测试工具已加载，使用 corsTest.runFullTest() 运行完整测试')
}