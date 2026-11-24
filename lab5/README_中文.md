# Java 实验5 — 中文使用说明

## 本实验到底做了什么？

按照老师的第5次实验要求，使用 **Swing + Java2D** 展示 **实验3的真实数据**：

- 集合：`ArrayList` 和 `HashMap`（完全继承你实验3用的选择）。
- 数量：10、100、1000、10000、100000。
- 四张图：添加平均时间、添加总时间、修改平均时间、修改总时间。
- **注意：实验3使用的是 set/replace「修改」，不是 remove「删除」！不能把数据错标成删除。**
- 数据单位是纳秒 `ns`；已附带从 GitHub Lab3 `results/summary.csv` 取出的真实汇总数据。

## 直接打开

1. 下载并解压 `Java_Lab5_Eclipse_Complete.zip`。
2. 打开 Eclipse，点 **File → Import → General → Existing Projects into Workspace → Next**。
3. 选择解压后 **`lab5` 文件夹**（里面有 `.project` 和 `.classpath`），点 Finish。
4. 确保 Eclipse 配置 JDK 8 或更新版本；选择 **Project → Clean**。
5. 找到 **`src → lab5 → Main.java`**，右键 **Run As → Java Application**。
6. 程序会自动显示四张图，无需自己先录入数字。

也可以在 Windows 安装 JDK 并配置好 PATH 后，双击 **`run_windows.bat`**。

## 七项要求怎么演示？

1. **缩放**：`+`、`−`、滑块、鼠标滚轮，拖动图形可以平移。
2. **图例**：勾选/取消“Легенда”。
3. **坐标轴**：X、Y 分别选 `LINEAR`（线性）或 `LOGARITHMIC`（对数）。
4. **载入文件**：按 `Открыть файлы...`，选 `*.csv`、`*.log` 或 `*.lab5`。
5. **分开/合并**：`Раздельно` 看单个文件，`Совместно` 叠加所有文件。导入实验3全部10个日志后，会自动组成2条完整曲线。
6. **鼠标悬停**：鼠标放在数据点上显示 N 和精确测量时间。
7. **保存设置**：点击 **`Сохранить данные + настройки...`**，保存为 `.lab5` 单文件。关闭程序，再载入该文件，数字、图例、缩放、坐标类型都会保留。

如果只是点击 **`Сохранить настройки`**，原 CSV/LOG 旁边会生成 `.view.properties`，不会乱改原始日志。要把所有内容交给老师，优先选单文件 `.lab5` 保存。

## 其他文件

- `data/lab3_summary.csv`：实验3真实汇总数据。
- `data/lab3_ready.lab5`：已经保存好数据和显示设置的单文件，可以直接用程序打开。
- `docs/DEFENSE_RU.md`：实验5九个答辩题的俄语答案、对应代码位置。
- `docs/preview.png`：四个图表的预览图。
- `docs/gui_smoke.png`：程序在图形环境实际打开的截图。
- `run_tests_windows.bat`：Windows 代码检查脚本。

**不要直接把实验2/3/5的 `.java` 混在同一个 Eclipse 源码目录**，容易造成重复包、版本冲突。这是独立的 `lab5` 项目，读取实验3数据，但不修改原实验3代码。
