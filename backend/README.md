# 聊天应用后端项目

这是一个基于Spring Boot的聊天应用后端项目，使用Maven进行构建管理。

## 技术栈

- **JDK**: 17
- **构建工具**: Maven 3.9.x
- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.4.2
- **ORM**: MyBatis-Plus 3.5.5
- **认证**: JWT
- **实时通信**: WebSocket
- **文件存储**: MinIO
- **音视频**: Coturn

## 项目结构

```
backend/
├── .mvn/                          # Maven Wrapper配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/chatapp/
│   │   │       ├── ChatBackendApplication.java  # 主启动类
│   │   │       ├── common/                      # 通用类
│   │   │       ├── config/                      # 配置类
│   │   │       ├── controller/                  # 控制器
│   │   │       ├── dto/                         # 数据传输对象
│   │   │       ├── entity/                      # 实体类
│   │   │       ├── exception/                   # 异常处理
│   │   │       ├── mapper/                      # MyBatis Mapper
│   │   │       ├── security/                    # 安全配置
│   │   │       ├── service/                     # 服务层
│   │   │       └── util/                        # 工具类
│   │   └── resources/
│   │       ├── mapper/                          # MyBatis XML映射文件
│   │       ├── sql/                             # 数据库脚本
│   │       └── application.yml                  # 应用配置
│   └── test/                                    # 测试代码
├── mvnw                                         # Maven Wrapper (Unix)
├── mvnw.cmd                                     # Maven Wrapper (Windows)
├── pom.xml                                      # Maven项目配置
└── README.md                                    # 项目说明
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.6+ (可选，项目包含Maven Wrapper)
- MySQL 8.0
- Redis 7.4.2

## 快速开始

### 1. 克隆项目
```bash
git clone <repository-url>
cd backend
```

### 2. 配置数据库
- 创建MySQL数据库: `chat_app`
- 执行初始化脚本: `src/main/resources/sql/init.sql`
- 修改 `application.yml` 中的数据库连接配置

### 3. 配置Redis
- 启动Redis服务
- 修改 `application.yml` 中的Redis连接配置

### 4. 编译项目
```bash
# 使用Maven Wrapper (推荐)
./mvnw clean compile

# 或使用本地Maven
mvn clean compile
```

### 5. 运行项目
```bash
# 使用Maven Wrapper
./mvnw spring-boot:run

# 或使用本地Maven
mvn spring-boot:run

# 或直接运行打包后的jar
./mvnw clean package
java -jar target/chat-backend.jar
```

### 6. 验证启动
项目启动后，访问: http://localhost:8080/api

## Maven命令

### 常用命令
```bash
# 清理项目
./mvnw clean

# 编译项目
./mvnw compile

# 运行测试
./mvnw test

# 打包项目
./mvnw package

# 跳过测试打包
./mvnw package -DskipTests

# 运行应用
./mvnw spring-boot:run

# 生成项目报告
./mvnw site
```

### 依赖管理
```bash
# 查看依赖树
./mvnw dependency:tree

# 分析依赖
./mvnw dependency:analyze

# 解决依赖冲突
./mvnw dependency:resolve
```

## API接口

### 认证相关
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新令牌
- `GET /api/auth/check-username` - 检查用户名
- `GET /api/auth/check-email` - 检查邮箱
- `GET /api/auth/check-phone` - 检查手机号

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_app
    username: root
    password: 123456
```

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

### JWT配置
```yaml
jwt:
  secret: chatAppSecretKey2024!@#$%^&*()
  expiration: 604800  # 7天
  refresh-expiration: 2592000  # 30天
```

## 开发建议

1. **代码规范**: 遵循Java编码规范，使用Lombok减少样板代码
2. **异常处理**: 使用全局异常处理器统一处理异常
3. **日志记录**: 合理使用日志级别，重要操作记录日志
4. **单元测试**: 为业务逻辑编写单元测试
5. **API文档**: 使用Swagger或类似工具生成API文档

## 故障排除

### 常见问题

1. **编译失败**: 检查JDK版本是否为17+
2. **数据库连接失败**: 检查MySQL服务是否启动，配置是否正确
3. **Redis连接失败**: 检查Redis服务是否启动
4. **端口占用**: 修改application.yml中的server.port配置

### 日志查看
```bash
# 查看应用日志
tail -f logs/application.log

# 查看Spring启动日志
./mvnw spring-boot:run --debug
```

## 许可证

本项目采用 [MIT许可证](LICENSE)