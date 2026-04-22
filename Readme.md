# LLMScoring

基于 `Spring Boot + JPA + MySQL + Vue 3` 的教学评分开源项目，面向教师出卷、学生在线答题、系统自动评分、教师复核发布成绩的完整业务链路。当前版本已经具备可运行的前后端原型，并支持启发式评分与 OpenAI-compatible LLM 评分两种模式。

## 项目定位

LLMScoring 关注的是“主观题教学评分”场景，重点覆盖以下流程：

- 教师注册、登录并维护个人资料
- 教师创建课程、试卷、题目与评分点
- 学生通过分享码免注册进入答题
- 系统对填空题和简答题进行自动评分
- 教师对自动评分结果进行人工复核与发布
- 学生使用分享码和学号查询最终成绩

## 当前已完成功能

### 教师端

- 教师注册、登录、个人资料维护
- 教师仪表盘，展示课程数、试卷数、待复核提交数、已发布结果数
- 课程创建与删除
- 试卷创建、编辑、删除
- 题目配置，当前支持 `FILL_BLANK` 与 `SHORT_ANSWER`
- 每题配置参考答案、评分点、分值与说明
- 试卷启停控制与分享码生成
- 分享页查看试卷入口、复制分享码、查看提交与评阅进度
- 教师评阅页查看自动评分结果、调整单题分数、填写评语、发布最终成绩

### 学生端

- 学生入口页支持“开始答题”和“查询成绩”两条路径
- 学生通过分享码免注册进入试卷
- 提交时填写学号，姓名可选
- 答题草稿自动保存在浏览器本地
- 提交后自动进入“等待复核/查看结果”流程
- 审核发布后查看每题得分、评语、评分依据与总分

### 评分能力

- 启发式评分服务，支持关键词匹配、得分点覆盖率与基础文本相似度判断
- OpenAI-compatible LLM 评分通道
- LLM 不可用时可自动回退到启发式评分
- 自动生成单题评语、评分依据与整体评语

### 工程能力

- Spring Boot REST API
- JPA + MySQL 持久化
- 前后端分离开发，前端构建后可由后端静态资源统一提供
- H2/MySQL 双环境支撑测试与运行
- 已包含核心流程测试与评分服务测试

## 技术栈

### 后端

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- MySQL 8
- H2（测试）
- Maven

### 前端

- Vue 3
- Vue Router
- Vite

## 快速启动

### 1. 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0+
- Node.js 20+（仅前端二次开发时需要）

### 2. 初始化数据库

默认数据库名为 `llm_scoring`。可以直接执行初始化脚本：

```powershell
mysql -uroot -p123456 < D:\idealcode\LLMScoring\database\mysql-init.sql
```

默认后端数据库连接如下：

- URL：`jdbc:mysql://localhost:3306/llm_scoring?...`
- 用户名：`root`
- 密码：`123456`

如果你希望自定义数据库地址，推荐通过环境变量覆盖：

```powershell
$env:LLM_SCORING_DB_URL="jdbc:mysql://localhost:3306/llm_scoring?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
```

### 3. 配置 LLM（可选）

项目默认启用了 OpenAI-compatible LLM 评分。如果你只是本地体验流程，可以先关闭 LLM，直接使用启发式评分：

```powershell
$env:LLM_SCORING_LLM_ENABLED="false"
```

如果需要接入真实模型服务，建议显式配置以下环境变量：

```powershell
$env:LLM_SCORING_LLM_ENABLED="true"
$env:LLM_SCORING_LLM_BASE_URL="https://your-openai-compatible-endpoint/v1"
$env:LLM_SCORING_LLM_API_KEY="your-api-key"
$env:LLM_SCORING_LLM_MODEL="your-model-name"
```

常用可选项：

- `LLM_SCORING_LLM_TEMPERATURE`
- `LLM_SCORING_LLM_MAX_COMPLETION_TOKENS`
- `LLM_SCORING_LLM_FALLBACK_TO_HEURISTIC`
- `LLM_SCORING_LLM_JSON_MODE`

### 4. 启动后端

```powershell
mvn spring-boot:run
```

启动后默认访问：

- 教师端入口：`http://localhost:8080/login`
- 学生端入口：`http://localhost:8080/student`

说明：

- 仓库中已经包含一次前端构建产物，直接启动后端即可体验基础流程
- 如果你修改了前端源码，请重新执行前端构建

### 5. 启动前端开发环境（可选）

```powershell
cd frontend
npm install
npm run dev
```

开发模式默认地址：

- 前端：`http://localhost:5173`
- API 代理到：`http://localhost:8080`

如需将前端重新打包到 Spring Boot 静态目录：

```powershell
cd frontend
npm run build
```

### 6. 运行测试

```powershell
mvn test
```

当前仓库已包含的测试重点：

- 评分服务测试
- 学生提交到教师复核发布的核心业务流程测试

## 默认演示信息

- 教师账号：`teacher`
- 教师密码：`teacher123`
- 演示分享码：`BIO-2026`

## 当前版本说明

当前版本已经能够完整演示“教师出卷 -> 学生答题 -> 系统评分 -> 教师复核 -> 学生成绩查询”的主流程，但仍属于开源原型阶段，以下能力还没有完全产品化：

- 忘记密码页面已预留，但未接入真实邮件重置流程
- 认证仍为演示型实现，尚未接入正式 Token / Session 方案
- 密码安全机制仍可继续增强
- 当前题型主要覆盖填空题与简答题
- 暂未提供成绩导出、批量导入、系统监控等增强能力

## 适合开源社区继续完善的任务

如果你希望参与贡献，下面这些方向都非常适合拆分成独立 Issue：

### 安全与生产化

- 接入 JWT / Session 等正式认证机制
- 密码加密存储、登录限流、审计日志
- 移除默认敏感配置，改为 `.env.example` 或部署文档引导
- 增加角色权限控制，如管理员、助教、教师等

### 评分能力增强

- 接入更多 OpenAI-compatible / 本地模型供应商
- 支持 Prompt 模板版本化与评分结果追踪
- 增加评分解释可视化与命中点高亮
- 引入异步评分队列、超时重试与批量评分能力
- 针对不同学科沉淀更丰富的评分模板

### 题型与业务扩展

- 新增选择题、判断题、名词解释、论述题等题型
- 支持试卷复制、题库复用、批量导入题目
- 支持成绩导出为 Excel / CSV
- 增加班级、学期、考试场次等维度
- 支持多教师协同维护课程与试卷

### 数据分析与教学支持

- 成绩统计图表与知识点掌握分析
- 班级维度排行榜、错题聚类、薄弱点分析
- 学生答案相似度检测与查重预警
- 历史成绩对比与学习轨迹分析

### 工程与开源协作

- Docker / Docker Compose 部署方案
- GitHub Actions 持续集成
- 前端单元测试与 E2E 测试
- API 文档自动化生成
- Issue 模板、PR 模板、贡献指南与代码规范

## 项目结构

```text
LLMScoring/
|-- src/main/java/com/llm/open/llmscoring
|   |-- controller    # REST API
|   |-- service       # 业务逻辑与评分服务
|   |-- repository    # 数据访问
|   |-- entity        # JPA 实体
|   `-- dto           # 接口模型
|-- src/main/resources
|   |-- static        # 前端构建产物
|   |-- prompts       # 评分提示词
|   |-- application.properties
|   `-- schema.sql
|-- src/test          # 测试代码
|-- frontend          # Vue 3 前端源码
|-- database          # MySQL 初始化脚本
|-- docs              # 设计与接口文档
`-- output/doc        # 生成的使用说明书
```

## 文档导航

- [接口文档](./docs/接口文档.md)
- [项目模块划分](./docs/项目模块划分.md)
- [数据库设计文档](./docs/数据库设计文档.md)
- [软件使用说明书（DOCX）](./docs/LLMScoring软件使用说明书.docx)

## 贡献建议

- 提交前请先运行 `mvn test`
- 若修改前端，请补充 `frontend` 下的构建验证
- 若新增题型或评分逻辑，建议同步补测试与文档

欢迎把这个项目继续打磨成真正可落地的教学评分开源系统。
