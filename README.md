# FileFinder

荣耀平板（Android 12+）快速文件名搜索工具。

## 特性
- 全盘文件名建立 SQLite 索引
- 拼音首字母搜索（输入 `bgs` 可以找到 "报告书.pdf"）
- 实时输入即搜索（150ms 防抖）
- 点击文件用系统默认应用打开
- 前台服务保活，避免 MagicOS 杀后台

## 编译 APK（无需本地 Android Studio）
本仓库已配置 GitHub Actions，每次推送自动云端编译。

1. 推送代码后，进入仓库的 **Actions** 页签
2. 点最新一次 `Build APK` 运行（绿色对勾代表成功）
3. 滚到底部 **Artifacts** 区域，下载 `FileFinder-debug-apk.zip`
4. 解压得到 `app-debug.apk`，传到平板安装

## 平板首次安装与运行

1. **允许未知来源安装**：设置 → 安全 → 更多安全设置 → 安装未知应用
2. **打开 APP**，点击"去授权"
3. 在系统页面找到 **FileFinder**，开启"允许访问所有文件"
4. 返回 APP，会自动开始扫描（通知栏显示进度）
5. **建议**：设置 → 应用 → FileFinder → 启动管理 → 全部允许（避免荣耀杀后台）
6. 索引完成后即可在搜索框输入查找

## 项目结构

```
app/src/main/
├── AndroidManifest.xml
└── java/com/lymcyc/filefinder/
    ├── MainActivity.kt          # Compose 搜索 UI
    ├── data/                    # Room 数据库
    ├── scanner/                 # 文件扫描 + 拼音转换
    ├── service/                 # 前台索引服务
    └── ui/theme/                # Material3 主题
```
