# 使用 GitHub Actions 构建 APK

## 概述

本项目已配置 GitHub Actions 自动构建 Android APK。每次推送到 GitHub 时，会自动构建 Debug 和 Release 版本的 APK。

## 快速开始

### 1. 创建 GitHub 仓库

1. 访问 https://github.com/new
2. 创建新仓库（例如：`goodwork-android`）
3. 不要初始化 README、.gitignore 或 license

### 2. 初始化本地 Git 仓库

```powershell
cd "C:\Users\Liuju\Desktop\codes\新建文件夹 (3)\goodwork"
git init
git add .
git commit -m "Initial commit: 好活 Android 应用"
```

### 3. 推送到 GitHub

```powershell
git remote add origin https://github.com/你的用户名/goodwork-android.git
git branch -M main
git push -u origin main
```

### 4. 触发构建

推送代码后，GitHub Actions 会自动开始构建：

1. 访问你的 GitHub 仓库
2. 点击 "Actions" 标签
3. 查看构建进度

### 5. 下载 APK

构建完成后（约 5-10 分钟）：

1. 在 Actions 页面找到完成的构建
2. 点击构建任务
3. 在页面底部找到 "Artifacts" 部分
4. 下载 `debug-apk` 或 `release-apk`

## 工作流详情

### 触发条件

- 推送到 `main` 或 `master` 分支
- 创建 Pull Request
- 手动触发（在 Actions 页面点击 "Run workflow"）

### 构建步骤

1. **Checkout 代码** - 获取源代码
2. **设置 JDK 17** - 配置 Java 环境
3. **授予执行权限** - 使 gradlew 可执行
4. **构建 Debug APK** - 生成调试版本
5. **构建 Release APK** - 生成发布版本
6. **上传 APK** - 保存构建产物（保留 30 天）

### 输出文件

- **Debug APK**: `app-debug.apk` - 用于测试和调试
- **Release APK**: `app-release.apk` - 用于正式发布

## 手动触发构建

如果需要在不推送代码的情况下构建：

1. 访问仓库的 "Actions" 页面
2. 选择 "Build Android APK" 工作流
3. 点击 "Run workflow" 按钮
4. 选择分支并运行

## 查看构建日志

1. 进入 Actions 页面
2. 点击具体的构建任务
3. 展开各个步骤查看详细日志

## 构建时间

- **首次构建**: 约 8-12 分钟（需要下载依赖）
- **后续构建**: 约 3-5 分钟（使用缓存）

## 常见问题

### Q: 构建失败怎么办？

A: 查看构建日志，常见原因：

1. **依赖下载失败** - 重试构建
2. **代码错误** - 检查提交的代码
3. **Gradle 版本问题** - 更新 `gradle-wrapper.properties`

### Q: 如何修改构建配置？

A: 编辑 `.github/workflows/build-apk.yml` 文件，然后推送更改。

### Q: APK 文件保留多久？

A: 默认保留 30 天，可以在工作流配置中修改 `retention-days`。

### Q: 可以构建特定变体吗？

A: 可以修改工作流文件，添加更多构建命令，例如：
```yaml
- name: Build specific variant
  run: ./gradlew assembleDebug assembleRelease assemblePaidRelease
```

### Q: 如何签名 Release APK？

A: 需要在 GitHub Secrets 中配置签名信息：

1. 在仓库设置中添加 Secrets：
   - `KEYSTORE_FILE` - Base64 编码的 keystore 文件
   - `KEYSTORE_PASSWORD` - keystore 密码
   - `KEY_ALIAS` - 密钥别名
   - `KEY_PASSWORD` - 密钥密码

2. 修改 `app/build.gradle` 配置签名

3. 更新工作流文件以使用这些 Secrets

## 高级配置

### 添加测试

```yaml
- name: Run tests
  run: ./gradlew test
```

### 生成代码覆盖率报告

```yaml
- name: Generate coverage report
  run: ./gradlew jacocoTestReport

- name: Upload coverage
  uses: codecov/codecov-action@v3
```

### 自动发布到 GitHub Releases

```yaml
- name: Create Release
  uses: softprops/action-gh-release@v1
  with:
    files: app/build/outputs/apk/release/app-release.apk
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## 项目结构

```
goodwork/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions 配置
├── app/
│   ├── build.gradle
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/goodwork/app/
│   │       └── res/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

## 下一步

1. ✅ 创建 GitHub 仓库
2. ✅ 初始化 Git 并推送代码
3. ✅ 等待 GitHub Actions 构建完成
4. ✅ 下载 APK 文件
5. ✅ 在 Android 设备上安装测试

## 技术支持

如遇问题，请检查：
- GitHub Actions 日志
- 仓库设置中的 Secrets 配置
- 工作流文件的语法是否正确

## 许可证

本项目仅供学习和个人使用。