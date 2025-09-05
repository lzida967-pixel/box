# Axios导入问题解决方案

## 问题描述
Vite开发服务器无法解析axios模块导入，错误信息：
```
[plugin:vite:import-analysis] Failed to resolve import "axios" from "src\api\config.ts"
```

## 解决方案

### 方案1：清除缓存并重新安装依赖
```bash
# 清除npm缓存
npm cache clean --force

# 删除node_modules和package-lock.json
rm -rf node_modules package-lock.json

# 重新安装依赖
npm install

# 清除vite缓存
rm -rf node_modules/.vite
```

### 方案2：使用具体版本的axios
在package.json中指定确切的axios版本：
```json
{
  "dependencies": {
    "axios": "1.6.0"
  }
}
```

### 方案3：修改vite配置
已在vite.config.ts中添加了optimizeDeps配置：
```typescript
export default defineConfig({
  // ... 其他配置
  optimizeDeps: {
    include: ['axios']
  }
})
```

### 方案4：使用动态导入（备用方案）
如果静态导入不工作，可以使用动态导入：
```typescript
// 替换静态导入
// import axios from 'axios'

// 使用动态导入
const axios = await import('axios')
const axiosInstance = axios.default || axios
```

### 方案5：重启开发服务器
有时简单的重启可以解决缓存问题：
```bash
# 停止当前运行的开发服务器 (Ctrl+C)
# 然后重新启动
npm run dev
```

## 验证解决方案

1. 确认axios已正确安装：
   ```bash
   npm list axios
   ```

2. 检查node_modules中是否存在axios目录：
   ```bash
   ls node_modules/axios
   ```

3. 启动开发服务器验证：
   ```bash
   npm run dev
   ```

## 预防措施

1. **保持依赖版本一致**：定期更新package-lock.json
2. **使用固定版本**：在package.json中使用确切版本号而不是范围
3. **定期清理缓存**：定期清理npm和vite缓存

## 技术说明

这个问题通常由以下原因引起：
1. **依赖安装不完整**：node_modules中缺少必要文件
2. **版本冲突**：package.json和实际安装的版本不匹配
3. **模块类型问题**：ESM/CommonJS模块解析冲突
4. **缓存问题**：vite或npm缓存损坏
5. **TypeScript配置**：模块解析配置不正确

## 当前状态

- ✅ axios依赖已在package.json中声明
- ✅ node_modules/axios目录存在
- ✅ vite.config.ts已添加optimizeDeps配置
- ✅ API配置文件添加了导入验证
- ⏳ 需要重启开发服务器验证修复效果