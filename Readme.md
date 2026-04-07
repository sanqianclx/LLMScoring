# 基于 LLM 的自动评分系统

面向教学场景的自动评分平台，支持教师创建课程与试卷，学生通过分享码在线答题，系统自动评分，教师审核并发布结果。项目采用 `Spring Boot + JPA + MySQL + Vue 3` 实现，已具备完整的前后端业务链路。

## 项目特点

- 教师端：注册、登录、课程管理、试卷管理、评分审核、个人中心
- 学生端：免登录答题、提交试卷、查看审核结果
- 支持填空题与简答题两种题型
- 支持按关键词与得分点进行自动评分
- 支持教师覆盖自动评分结果，形成最终成绩
- 前端使用 Vue 3 路由化页面，后端提供 Spring Boot REST API
- 数据采用 MySQL 持久化存储

## 技术栈

### 后端

- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- H2（测试环境）
- Maven

### 前端

- Vue 3
- Vue Router
- Vite

## 核心业务流程

### 教师端

1. 教师注册或登录
2. 创建课程
3. 在课程下创建试卷并配置题目、参考答案、评分点
4. 生成分享码并发送给学生答题
5. 查看学生提交与自动评分结果
6. 人工审核并发布最终成绩

### 学生端

1. 通过分享码进入答题页面
2. 输入学号并完成答题
3. 提交试卷
4. 等待教师审核
5. 查看每题得分、评语、评分依据与总分

## 项目结构

```text
LLMScoring/
├─ src/main/java/com/llm/open/llmscoring
│  ├─ controller   # 接口控制层
│  ├─ service      # 业务逻辑层
│  ├─ repository   # 数据访问层
│  ├─ entity       # JPA 实体
│  └─ dto          # 数据传输对象
├─ src/main/resources
│  ├─ static       # 前端构建产物
│  ├─ application.properties
│  └─ schema.sql
├─ src/test/java   # 后端测试
├─ frontend        # Vue 3 前端源码
├─ database        # 数据库初始化脚本
└─ docs            # 项目文档
```

## 快速启动

### 1. 初始化数据库

项目当前默认连接 MySQL：

- 用户名：`root`
- 密码：`123456`
- 数据库：`llm_scoring`

可直接执行：

```powershell
mysql -uroot -p123456 < D:\idealcode\LLMScoring\database\mysql-init.sql
```

### 2. 启动后端

```powershell
mvn spring-boot:run
```

### 3. 单独启动前端开发环境（可选）

```powershell
cd frontend
npm install
npm run dev
```

### 4. 运行测试

```powershell
mvn test
```

## 演示账号

- 教师账号：`teacher`
- 教师密码：`teacher123`
- 演示分享码：`BIO-2026`

## 文档导航

- [接口文档](./docs/接口文档.md)
- [项目模块划分](./docs/项目模块划分.md)

## 当前实现说明

当前版本已完成完整业务闭环，但仍属于课程项目型实现：

- 自动评分模块当前为启发式评分逻辑，后续可替换为真实 LLM API
- 教师密码当前为明文存储，后续建议改为加密存储
- 认证方式为轻量实现，后续建议补充正式权限与 Token 机制

## 后续可扩展方向

- 接入 DeepSeek、GPT 等真实大模型评分服务
- 增加选择题、判断题等题型
- 支持成绩导出与统计分析
- 支持多教师协同管理课程
- 增加查重与答案相似度检测
