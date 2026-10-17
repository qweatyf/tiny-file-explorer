# MyEmptyActivity

A small file manager. Open source. Free for everyone.

Features

File manager: browse, copy, move, rename, delete, zip, unzip
File tools: encryption, batch rename, file search, file compare, hash check, split / merge, empty dir clean
Image tools: compress, format convert, watermark, color picker, GIF make / decode
Audio / video: media player, video frame capture, audio trim, video to audio, audio info
Document tools: Markdown preview, CSV view, text encoding, PDF merge
Crypto tools: AES encrypt / decrypt, hash calculator, password generator
System tools: CPU / memory / battery / network monitor, storage analysis, app detail, app uninstall, app freeze, app clean, auto start manager
Network tools: HTTP request, LAN scan, port scan, speed test
Utilities: unit converter, color picker, QR generate / scan, QR batch, clock, countdown days, stopwatch, countdown timer, alarm clock, account book
Local chat: LAN chat, quick transfer
Remote mode: configure your own server, chat and transfer over the internet
3D view: 3D monitor for CPU / battery, 3D earth
GitHub upload: upload a local directory to GitHub
Terminal: Morse terminal, real terminal
Others: memo, vault, real draw, hardcore draw

Highlights

Chinese and English, switchable
Local mode by default, remote mode optional
Bring your own server, data never touches a third party
Open source, commercial use allowed, just keep the attribution

Build

Requirements: Android SDK, JDK 17, Gradle.

Open the project with Android Studio and build normally. Or build from the command line:

    ./gradlew assembleDebug

The APK will be in app/build/outputs/apk/debug/.

Third-party libraries

Three.js, MIT, https://threejs.org/
NanoHTTPD, BSD 3-Clause, https://github.com/NanoHttpd/nanohttpd
zxing core, Apache 2.0, https://github.com/zxing/zxing
zxing-android-embedded, Apache 2.0, https://github.com/journeyapps/zxing-android-embedded
pdfbox-android, Apache 2.0, https://github.com/TomRoush/PdfBox-Android
pinyin4j, GPL, https://sourceforge.net/projects/pinyin4j/
NASA Blue Marble, Public Domain, https://visibleearth.nasa.gov/collection/1484/blue-marble

Note: pinyin4j is licensed under GPL. This project as a whole is under a custom license, but the pinyin4j part is subject to GPL. If you want to use this project for closed-source commercial purposes, you need to replace pinyin4j or comply with GPL.

License

This project uses a custom open source license:

Free to use, modify, commercial use allowed
Attribution to "常见" required
Contact: ygrudh@proton.me

See the LICENSE file for details.

Author

常见
Email: ygrudh@proton.me

MyEmptyActivity

小型文件管理器，开源，纯公益。

功能

文件管理：浏览、复制、移动、重命名、删除、压缩、解压
文件工具：文件加密、批量重命名、文件搜索、文件对比、哈希校验、切分合并、空目录清理
图片工具：压缩、转格式、加水印、取色、GIF 合成 / 拆帧
音视频：音视频播放、视频抽帧、音频裁剪、视频转音频、音频信息
文档工具：Markdown 预览、CSV 查看、文本编码、PDF 合并
加密工具：AES 加解密、哈希计算、密码生成
系统工具：CPU / 内存 / 电池 / 网络监控、存储分析、应用详情、应用卸载、应用冻结、应用清理、自启管理
网络工具：HTTP 请求、局域网扫描、端口扫描、网速测试
实用工具：单位换算、颜色选择器、二维码生成 / 扫描、二维码批量、钟表、倒数日、秒表、倒计时、定时闹钟、记账本
本地聊天：局域网聊天、快传
远程模式：可配置自己的服务器，异地也能聊天、传文件
3D 视图：CPU / 电池 3D 监控，3D 地球
GitHub 上传：把本地目录一键推到 GitHub
终端：摩斯电码终端、真终端
其他：备忘录、保险箱、真正画画、硬核画画

特色

中英双语，可切换
默认本地模式，可配置服务器走远程
服务器自己搭，数据不过第三方
开源，可商用，按协议署名即可

编译

环境要求：Android SDK、JDK 17、Gradle。

用 Android Studio 打开项目，正常编译即可。或者用命令行：

    ./gradlew assembleDebug

生成的 APK 在 app/build/outputs/apk/debug/ 目录下。

第三方库

Three.js, MIT, https://threejs.org/
NanoHTTPD, BSD 3-Clause, https://github.com/NanoHttpd/nanohttpd
zxing core, Apache 2.0, https://github.com/zxing/zxing
zxing-android-embedded, Apache 2.0, https://github.com/journeyapps/zxing-android-embedded
pdfbox-android, Apache 2.0, https://github.com/TomRoush/PdfBox-Android
pinyin4j, GPL, https://sourceforge.net/projects/pinyin4j/
NASA Blue Marble, Public Domain, https://visibleearth.nasa.gov/collection/1484/blue-marble

注意：pinyin4j 采用 GPL 协议。本项目整体采用自定义协议，但 pinyin4j 部分受其 GPL 协议约束。若你希望将本项目用于闭源商用，需自行替换 pinyin4j 或遵守 GPL 的相关要求。

协议

本项目采用自定义开源协议：

可自由使用、修改、商用
需保留署名「常见」
联系方式：ygrudh@proton.me

详见 LICENSE 文件。

作者

常见
邮箱：ygrudh@proton.me