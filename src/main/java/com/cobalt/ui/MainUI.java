package com.cobalt.ui;

import com.cobalt.common.constant.Constants;
import com.cobalt.common.constant.UIConstants;
import com.cobalt.common.enums.ToastType;
import com.cobalt.common.utils.ModuleUtils;
import com.cobalt.common.utils.ReadingUtils;
import com.cobalt.common.utils.ToastUtils;
import com.cobalt.framework.factory.BeanFactory;
import com.cobalt.framework.factory.ViewFaction;
import com.cobalt.framework.persistence.proxy.ReadSubscriptProxy;
import com.cobalt.framework.persistence.proxy.ReadingProgressProxy;
import com.cobalt.framework.persistence.proxy.SettingsParameterProxy;
import com.cobalt.framework.persistence.proxy.SpiderActionProxy;
import com.cobalt.parser.book.BookMetadata;
import com.cobalt.parser.book.BookParserFacade;
import com.cobalt.parser.book.BooksWorker;
import com.cobalt.parser.chapter.Chapter;
import com.cobalt.parser.chapter.ChapterWorker;
import com.cobalt.parser.content.ContentWorker;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.rabbit.foot.loader.Resources;
import com.rabbit.foot.utils.ObjUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

/**
 * <p>
 * 首页窗口
 * </p>
 *
 * @author LiAo
 * @since 2021/1/14
 */
public class MainUI {

    private final static Logger log = LoggerFactory.getLogger(MainUI.class);

    // 搜索按钮
    private JButton btnSearch;
    // 搜索文本框
    private JTextField textSearchBar;
    // 窗口
    private JPanel mainPanel;
    // 开始阅读按钮
    private JButton openBook;
    // 搜索书本新信息
    private JTable searchBookTable;
    // 上一章按钮
    private JButton btnOn;
    // 下一章按钮
    private JButton underOn;
    // 章节跳转按钮
    private JButton jumpButton;
    // 章节内容
    private JEditorPane textContent;
    // 章节内容外部框
    private JScrollPane paneTextContent;
    // 章节目录下拉列表
    private JComboBox<String> chapterList;
    // 表格外围
    private JScrollPane tablePane;
    // 搜索下拉列表数据源
    private JComboBox<String> sourceDropdown;
    // 设置按钮
    private JButton settingBtn;
    // 表格与文本内容分隔栏
    private JSplitPane bookTabContentSplit;

    // 全局模块对象
    public final Project project;
    // 书籍链接
    private String valueAt;
    // 用于判断是否是当前选项卡切换
    private Content lastSelectedContent = null;
    // 是否切换了书本（是否点击了开始阅读按钮）
    public static boolean isReadClick = false;

    // 阅读进度持久化
    private final ReadingProgressProxy readingProgress;

    // 阅读窗口滚动位置持久化
    private final ReadSubscriptProxy readSubscript;

    // 页面设置持久化
    private final SettingsParameterProxy settingsParameter;
    // 爬虫资源配置项
    private final SpiderActionProxy spiderAction;
    // 书籍导入处理类
    static BookParserFacade bookParserFacade = (BookParserFacade) BeanFactory.getBean("BookParserFacade");


    // 初始化数据
    private void init() {
        // 初始化表格
        searchBookTable.setModel(UIConstants.tableModel);
        searchBookTable.setEnabled(true);
        // 加载数据源下拉框
        loadDataOrigin();
        // 设置表格内容大小
        tablePane.setPreferredSize(new Dimension(-1, 30));
        // 加载组件配置信息
        ModuleUtils.loadModuleConfig(paneTextContent);
        // 加载提示信息
        ModuleUtils.loadComponentTooltip(btnSearch, openBook, settingBtn, btnOn, underOn, jumpButton);
        // 加载持久化的设置
        ModuleUtils.loadSetting(paneTextContent, textContent, bookTabContentSplit);
        // 存储窗口组件
        BookMetadata.getInstance().setTextContent(textContent);
        // 加载阅读进度
        ReadingUtils.loadReadingProgress(chapterList, textContent);
        // 页面回显
        if (readingProgress.getSearchType().equals(UIConstants.IMPORT) &&
                readingProgress.getBookType().equals(Constants.EPUB_STR_LOWERCASE)) {
            new ChapterWorker(project, textContent, chapterList, mainPanel).execute();
        }
    }

    // 页面初始化加载
    public MainUI(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.readingProgress = new ReadingProgressProxy();
        this.readSubscript = new ReadSubscriptProxy();
        this.settingsParameter = new SettingsParameterProxy();
        this.spiderAction = new SpiderActionProxy();
        // 执行初始化表格
        init();
        // 搜索
        btnSearch.addActionListener(e -> searchBook());
        // 书籍搜索框键盘按键事件
        textSearchBar.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 检查按下的键是否是回车键
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchBook();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });


        // 开始阅读
        openBook.addActionListener(e -> {
            // 等待鼠标样式
            ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
            // 获取选中行数据
            int selectedRow = searchBookTable.getSelectedRow();
            if (selectedRow < 0) {
                ToastUtils.showToastMassage(project, "还没有选择要读哪本书", ToastType.ERROR);
                // 恢复默认鼠标样式
                ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
                return;
            }
            // 获取数据源类型
            readingProgress.setSearchType(Objects.requireNonNull(sourceDropdown.getSelectedItem()).toString());
            // 获取书籍链接
            valueAt = searchBookTable.getValueAt(selectedRow, 4).toString();
            // 执行开始阅读
            new ContentWorker(valueAt, chapterList, project, textContent, mainPanel).execute();
            // 阅读进度持久化
            /*ReadingProgress.getInstance().loadState();*/
        });


        // 上一章节跳转
        btnOn.addActionListener(e -> {
            ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
            if (readingProgress.getChapters().isEmpty() || readingProgress.getNowChapterIndex() == 0) {
                ToastUtils.showToastMassage(project, "已经是第一章了", ToastType.ERROR);
                // 恢复默认鼠标样式
                ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
                return;
            }
            readingProgress.setNowChapterIndex(readingProgress.getNowChapterIndex() - 1);
            // 加载阅读信息
            new ChapterWorker(project, textContent, chapterList, mainPanel).execute();
            // 阅读进度持久化
            /*ReadingProgress.getInstance().loadState();*/
        });


        // 下一章跳转
        underOn.addActionListener(e -> {
            // 等待鼠标样式
            ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
            if (readingProgress.getChapters().isEmpty() ||
                    readingProgress.getNowChapterIndex() == readingProgress.getChapters().size() - 1) {
                ToastUtils.showToastMassage(project, "已经是最后一章了", ToastType.ERROR);
                // 恢复默认鼠标样式
                ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
                return;
            }
            // 章节下标加一
            readingProgress.setNowChapterIndex(readingProgress.getNowChapterIndex() + 1);
            // 加载阅读信息
            new ChapterWorker(project, textContent, chapterList, mainPanel).execute();
            // 阅读进度持久化
            /*ReadingProgress.getInstance().loadState();*/
        });


        // 章节跳转
        jumpButton.addActionListener(e -> {
            // 等待鼠标样式
            ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
            // 根据下标跳转
            readingProgress.setNowChapterIndex(chapterList.getSelectedIndex());
            if (readingProgress.getChapters().isEmpty() ||
                    readingProgress.getNowChapterIndex() < 0) {
                ToastUtils.showToastMassage(project, "未知章节", ToastType.ERROR);
                // 恢复默认鼠标样式
                ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
                return;
            }
            // 加载阅读信息
            new ChapterWorker(project, textContent, chapterList, mainPanel).execute();

            // 阅读进度持久化
            /*ReadingProgress.getInstance().loadState();*/
        });


        // 阅读滚动
        paneTextContent.getVerticalScrollBar().addAdjustmentListener(e -> {
            int textWinIndex = paneTextContent.getVerticalScrollBar().getValue();
            if (!(textWinIndex <= 0)) {
                readSubscript.setHomeTextWinIndex(textWinIndex);
            }
        });


        // 窗口加载结束
        ApplicationManager.getApplication().invokeLater(() -> {
            paneTextContent.getVerticalScrollBar().setValue(readSubscript.getHomeTextWinIndex());
            // 窗口未初始化
            if (project.isDisposed() || toolWindow == null) return;
            final ContentManager contentManager = toolWindow.getContentManager();
            // 监听当前选中的面板 进行阅读进度同步
            contentManager.addContentManagerListener(new ContentManagerListener() {
                @Override
                public void selectionChanged(@NotNull ContentManagerEvent event) {
                    Content selectedContent = event.getContent();
                    if (readingProgress.getChapters().isEmpty() ||
                            selectedContent == lastSelectedContent) return;
                    // 同步字体等设置
                    ModuleUtils.loadSetting(paneTextContent, textContent, bookTabContentSplit);
                    ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
                    // 只有选择的内容面板发生变化时才进行相关操作
                    lastSelectedContent = selectedContent;
                    if (selectedContent.getDisplayName().equals(UIConstants.TAB_CONTROL_TITLE_HOME)) {
                        // 获取新的章节位置
                        Chapter chapter = readingProgress.getChapters().get(readingProgress.getNowChapterIndex());
                        // 页面回显
                        if (readingProgress.getSearchType().equals(UIConstants.IMPORT) &&
                                readingProgress.getBookType().equals(Constants.EPUB_STR_LOWERCASE)) {
                            BookMetadata bookData = BookMetadata.getInstance();
                            bookData.setTextContent(textContent);
                            textContent.setDocument(bookData.getBookHTMLDocument());
                        } else {
                            // 章节内容赋值
                            String htmlContent = ModuleUtils.fontSizeFromHtml(settingsParameter.getFontSize(), readingProgress.getTextContent());
                            textContent.setText(htmlContent);
                        }
                        // 设置下拉框的值
                        chapterList.setSelectedItem(chapter.getName());
                        // 回到顶部
                        textContent.setCaretPosition(1);
                    }
                    ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
                }
            });
            toolWindow.installWatcher(contentManager);
        });


        // 设置单击事件
        settingBtn.addActionListener(i -> {
            try {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Cobalt_Settings");
            } catch (Exception e) {
                ToastUtils.showToastMassage(project, "Settings Page Error", ToastType.ERROR);
                log.error("Settings Error {}", e.getMessage());
            }
        });

        // 分割面板变动
        bookTabContentSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            settingsParameter.setSplitPosition((int) evt.getNewValue());
        });
    }


    /**
     * 搜索书籍
     */
    private void searchBook() {
        // 等待鼠标样式
        ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.WAIT_CURSOR);
        // 清空表格数据
        UIConstants.tableModel.setRowCount(0);
        // 获取搜索输入文本
        // 搜索书籍名称
        String bookSearchName = textSearchBar.getText();

        if (bookSearchName == null || bookSearchName.trim().isEmpty()) {
            ToastUtils.showToastMassage(project, "请输入书籍名称", ToastType.ERROR);
            // 等待鼠标样式
            ModuleUtils.loadTheMouseStyle(mainPanel, Cursor.DEFAULT_CURSOR);
            return;
        }
        // 获取数据源类型
        readingProgress.setSearchType(Objects.requireNonNull(sourceDropdown.getSelectedItem()).toString());
        readingProgress.setBookType(UIConstants.NETWORK);
        new BooksWorker(bookSearchName, project, mainPanel).execute();
    }


    /**
     * 应用滚动速度滑块
     */
    private void applyScrollSpacing() {
        SettingsUI settingsUI = (SettingsUI) BeanFactory.getBean("SettingsUI");
        paneTextContent.getVerticalScrollBar().setUnitIncrement(settingsUI.getReadRollVal());
        // 还原滚动位置
        // textContent.setCaretPosition(readSubscriptDao.homeTextWinIndex);
        // 持久化
        settingsParameter.setScrollSpacingScale(settingsUI.getReadRollVal());
    }


    /**
     * 书籍导入
     */
    private void applyImportBook() {
        SettingsUI settingsUI = (SettingsUI) BeanFactory.getBean("SettingsUI");
        /*ReadingProgress.getInstance().loadState();*/
        if (settingsUI.isSelBook) {
            readingProgress.setImportPath(settingsUI.getImportBookPath());
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(readingProgress.getImportPath());
            if (file == null) {
                ToastUtils.showToastMassage(project, "文件不存在", ToastType.ERROR);
                return;
            }
            if (!bookParserFacade.initBook(file)) {
                ToastUtils.showToastMassage(project, "书籍导入失败", ToastType.ERROR);
                return;
            }
            readingProgress.setSearchType(UIConstants.IMPORT);
            // 执行开始阅读
            new ContentWorker(valueAt, chapterList, project, textContent, mainPanel).execute();
            // 阅读进度持久化
            /*ReadingProgress.getInstance().loadState();*/
        }
    }

    /**
     * 加载数据源下拉框
     */
    private void loadDataOrigin() {
        sourceDropdown.removeAllItems();
        // 加载数据源下拉框
        ViewFaction.initSpiderConfig();
        if (ObjUtil.isEmpty(spiderAction.getSpiderActionStr())) {
            sourceDropdown.addItem(UIConstants.DEFAULT_DATA_SOURCE_NAME);
            return;
        }
        Resources.getObjectNode(spiderAction.getSpiderActionStr());
        for (String dataSourceName : Resources.getResourceNames()) {
            sourceDropdown.addItem(dataSourceName);
        }
    }


    /**
     * 页面统一的Apply
     */
    public void apply() {
        // 导入的书籍展示
        applyImportBook();
        // 字体大小
        ModuleUtils.applyFontSize(textContent);
        // 滑块滚动
        applyScrollSpacing();
        // 加载数据源下拉框
        loadDataOrigin();
    }


    // 窗口信息
    public JPanel getMainPanel() {
        return mainPanel;
    }


    private void createUIComponents() {
    }
}

