# ЛР №5 — ответы на контрольные вопросы и указатели в коде

## 1. В чём отличие библиотек Swing и AWT?

**AWT** использует в том числе нативные компоненты ОС («тяжеловесные»). **Swing** реализует большинство элементов интерфейса средствами Java («легковесные»), поддерживает единообразное оформление и наследует ряд классов AWT. В работе применены `JFrame`, `JComponent`, `JButton`, `JComboBox`, `JSlider` и `Graphics2D`.

## 2. Что такое фрейм?

**Фрейм** — главное окно приложения. В Swing это объект класса `JFrame`. В работе: `src/lab5/view/MainFrame.java`, объявление `extends JFrame`, конструктор `MainFrame(...)`.

## 3. Что такое компонент?

**Компонент** — отдельный элемент GUI с визуальным представлением и обработкой событий. В работе: `GraphPanel extends JComponent` — пользовательский компонент, рисующий график.

## 4. Каков порядок создания простейшего интерфейса?

1. Создать окно (`JFrame`).
2. Создать графический компонент (`JComponent`).
3. Добавить компонент в контейнер (`JPanel` и `setContentPane`).
4. Задать компоновку и размеры, сделать фрейм видимым.

Вход выполняется на **EDT** через `EventQueue.invokeLater` в `src/lab5/Main.java`. Это обеспечивает корректную работу Swing.

## 5. Для чего предназначена панель ContentPane?

`ContentPane` — контейнер содержимого окна, в котором располагаются остальные компоненты. Код: `src/lab5/view/MainFrame.java`, метод `createInterface()`, строка `setContentPane(content)`.

## 6. Как создать объект для двумерного рисования?

Метод `paintComponent(Graphics graphics)` переопределяется в `JComponent`, затем используется контекст типа `Graphics2D`:

```java
@Override protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    Graphics2D g2 = (Graphics2D) graphics.create();
    try {
        paintChart(g2);
    } finally {
        g2.dispose();
    }
}
```

В работе: `src/lab5/view/GraphPanel.java`, метод `paintComponent`; рисование отрезков `Line2D.Double` и точек `Ellipse2D.Double`.

## 7. Как установить имя и размер шрифта?

```java
g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
```

В работе используется `getFont().deriveFont(Font.BOLD, 15f)` и `g2.setFont(...)` в `GraphPanel.paintChart()`.

## 8. Как узнать список доступных шрифтов?

```java
String[] names = java.awt.GraphicsEnvironment
    .getLocalGraphicsEnvironment()
    .getAvailableFontFamilyNames();
```

Этот вызов относится к стандартному API Java; в приложении список не нужен, поэтому не создаётся бессмысленная кнопка для его вывода.

## 9. Что такое графический контекст?

**Графический контекст** — объект `Graphics` / `Graphics2D`, содержащий настройки и операции рисования: цвет, шрифт, масштаб, форму линий, отсечение и т. д. Он передаётся методу `paintComponent` при перерисовке компонента.

## Возможные дополнительные вопросы преподавателя

**Где данные ЛР3?** — `data/lab3_summary.csv`; чтение `MeasurementFileService.readDelimited`. Это реальные результаты `lab3/results/summary.csv` в GitHub.

**Почему не удаление?** — в ЛР3 измерены `ArrayList.set` и `HashMap.replace`; нельзя подписывать их как `remove`.

**Как работает логарифмическая ось?** — значения преобразуются через `Math.log10` в `GraphPanel.drawX()` и `drawY()`. На логарифмической оси Y нулевые значения не отображаются, поскольку `log(0)` не определён.

**Где масштаб?** — `MainFrame.zoomSlider`, `GraphPanel.drawX/drawY`, объект `ViewSettings`. Изменение масштаба влияет на обе оси.

**Где сохранение настроек?** — `MeasurementFileService.saveBundle`, `saveSettings`, `readDelimited` и `applySettingLine`. Для `.lab5` данные и параметры находятся в одном файле.

**Как реализовано разделение/совмещение?** — `MainFrame.redraw()`. В раздельном режиме в график передаётся один `DataFile`, в совместном — список всех файлов. `GraphPanel.createSeries()` объединяет отдельные LOG одной коллекции в линию.

**Почему обработка больших LOG не блокирует интерфейс?** — `MainFrame.chooseFiles()` применяет `SwingWorker`: чтение выполняется вне потока EDT, а обновление GUI — в `done()` на EDT.

**Как обрабатываются ошибки?** — `ErrorHandler.register()` ведёт счётчик, пишет текст в `lab5_errors.log` и возвращает сообщение для диалога `JOptionPane`.

**Что означает среднее время?** — `Average = Total / count`. Для ЛР3 количество добавлений `N`, количество изменений `N/10`. Все времена в нс.
