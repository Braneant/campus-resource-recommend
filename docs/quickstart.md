# 从全新克隆启动

此说明面向本地开发和演示。需要 Java 17 或更高版本、Maven 3.9、Node.js 22.12 或更高版本、Python 3、MySQL 8 和 Neo4j。Neo4j 本身所需 Java 版本以其发行版要求为准。本地脚本使用 Windows；其他平台可按下面的 Maven 和 npm 命令手动运行。

1. 克隆仓库，在 MySQL 创建空数据库 `campus_recommend`，按 `data/README.md` 顺序导入 SQL；在隔离的 Neo4j 数据库导入 `data/neo4j_init.cypher`。这些是模拟数据，请勿导入其他项目使用的业务数据库。
2. 将 `backend/local-env.example.cmd` 复制为 `backend/local-env.cmd`，填写 `MYSQL_PASSWORD`、`NEO4J_PASSWORD` 和足够长的随机 `JWT_SECRET`。本地密钥文件已被 Git 忽略。后端默认连接 `localhost:3306/campus_recommend` 和 `bolt://localhost:7687`。
3. 在仓库根目录执行：

   ```powershell
   mvn -B -f backend/pom.xml package
   cd frontend
   npm ci
   cd ..
   ```

4. 执行 `一键启动.bat /check` 检查本地工具、配置和构建产物；双击 `一键启动.bat` 启动，按窗口显示的地址访问。端口被占用时启动器会选择空闲端口并同步前端代理。`/diag` 检查本启动器记录的进程及接口，`停止服务.bat` 只停止身份匹配的项目进程。

可在本地环境文件中设置 `CAMPUS_JAVA`、`CAMPUS_NODE` 指定可执行文件，或设置 `JAVA_HOME`；需要由脚本启动 Neo4j 时可设置 `NEO4J_HOME` 和 `NEO4J_JAVA_HOME`。更换数据库地址时使用 `SPRING_DATASOURCE_URL` 和 `NEO4J_URI`，启动器和后端均读取相同地址。MySQL 和 Neo4j 是独立服务，停止脚本不会关闭它们。

密码找回需要真实 SMTP。可在忽略的 `backend/local-env.cmd` 中设置 `SPRING_MAIL_HOST`、`SPRING_MAIL_PORT`、`SPRING_MAIL_USERNAME`、`SPRING_MAIL_PASSWORD`、`SECURITY_PASSWORD_RESET_MAIL_FROM`，并按照邮件供应商要求配置 `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` 和 `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE`。未配置邮件时，密码找回会明确提示不可用；请勿提交真实凭据。首次启动需允许数据库创建 `password_reset_credential` 表；受限账户可由管理员执行 `backend/src/main/resources/db/migration/V20261102_01__password_reset_credential.sql`，再配置关闭自动建表。

CI 运行后端测试、前端构建及 Windows/Linux 启动器安全测试。上述本机端口检查不能证明远程数据库凭据和数据完整性，实际启动时还会检查资源查询接口。
