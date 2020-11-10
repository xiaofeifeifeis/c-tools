package com.bixuebihui.jmesa;


import com.bixuebihui.jdbc.IBaseListService;
import com.bixuebihui.jdbc.SqlFilter;
import com.bixuebihui.jdbc.ClobString;
import com.bixuebihui.jdbc.SqlSort;
import com.bixuebihui.jsp.TimeSpan;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmesa.limit.*;
import org.jmesa.model.TableModel;
import org.jmesa.model.TableModelUtils;
import org.jmesa.model.WorksheetSaver;
import org.jmesa.view.component.Column;
import org.jmesa.view.component.Row;
import org.jmesa.view.component.Table;
import org.jmesa.view.editor.CellEditor;
import org.jmesa.view.html.component.HtmlColumn;
import org.jmesa.worksheet.*;
import org.jmesa.worksheet.editor.CheckboxWorksheetEditor;
import org.jmesa.worksheet.editor.WorksheetCheckboxHeaderEditor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

/**
 * @author xwx
 */
public abstract class AbstractWebUI<T, V> implements WorksheetSaver {

    public static final String VAR_NAME = "row";
    private static final String TABLE_I18N = "tables";
    protected String id; // The unique table id.
    protected String successView;
    protected IBaseListService<T, V> service;
    protected Log mLog = LogFactory.getLog(AbstractWebUI.class);
    protected String checkboxName = "chk";
    protected String actionParam = "ac";
    protected String editableParam = "editable";
    protected Map<String, String> colsTemplate;
    protected static ConvertUtilsBean converter = new ConvertUtilsBean();
    protected static Log log = LogFactory.getLog(BasicWebUI.class);

    protected abstract String[] getColNames();

    protected String tableCaption = null;

    public void setTitles(org.jmesa.view.component.Table table,
                          String tableName, String[] colNames, boolean skipFirst) {

        table.setCaption(this.getTableCaption(tableName));


        ResourceBundle bundle = ResourceBundle.getBundle(TABLE_I18N);

        int start = skipFirst ? 1 : 0;
        for (int i = start; i < colNames.length; i++) {
            String key = colNames[i];
            try {
                key = bundle.getString(tableName + "." + colNames[i]);
            } catch (Exception e) {
                mLog.warn(e.getMessage());
            }
            table.getRow().getColumn(colNames[i]).setTitle(key);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setService(IBaseListService<T, V> service) {
        this.service = service;
    }

    public AbstractWebUI() {
    }

    protected abstract V[] getKeys(HttpServletRequest request);

    protected String render(HttpServletRequest request,
                            HttpServletResponse response) throws SQLException {
        TableModel tableModel = new TableModel(id, request, response);

        String ac = request.getParameter(actionParam);
        mLog.debug("AbstractWebUI " + actionParam + " = " + ac);

        return baseRender(tableModel,
                "true".equals(request.getParameter(editableParam)), ac,
                getKeys(request));
    }

    protected void onSetCustomToolBar(CustomToolbar toolbar) {
        toolbar.addButton(CustomToolbar.RUN);
    }

    protected void onRun(TableModel tableModel, V[] keys) {

    }

    protected String baseRender(TableModel tableModel, boolean editable,
                                String action, V[] keys) throws SQLException {
        tableModel.setEditable(editable);
        this.performAction(action, tableModel, keys);

        Table table = tableModel.isExporting() ? TableModelUtils
                .createTable(getColNames()) : TableModelUtils
                .createHtmlTable(getColNames());

        tableModel.setExportTypes(TableModel.CSV, TableModel.EXCEL); // Tell the tableFacade what
        // exports to use.
        tableModel.setStateAttr("restore");
        setDataAndLimitVariables(tableModel); // Find the data to display and
        // build the Limit.
        CustomToolbar toolbar = new CustomToolbar();

        toolbar.enablePageNumbers(true);
        onSetCustomToolBar(toolbar); //allow user add additional custom tool bar buttons
        tableModel.setToolbar(toolbar);


        // 表名，字段名等保存在tables_zh_CN.properties的文件中
        this.setTitles(table, id, getColNames(), true);
        final Row row = table.getRow();
        row.setUniqueProperty(getUniquePropertyName());

        tableModel.setTable(table);

        if (tableModel.isExporting()) {
            exportRender(tableModel);
            // Will write the export data out to the response.
            return null; // In Spring return null tells the controller not to do
            // anything.
        } else {
            HtmlColumn chkbox = (HtmlColumn) row.getColumn("chkbox");
            chkbox.setHeaderEditor(new WorksheetCheckboxHeaderEditor());

            chkbox.setCellEditor((item, property, rowcount) -> {

                UniqueProperty rowid = row.getUniqueProperty(item);
                String v = rowid == null ? "no_unique_property" : rowid.getValue();

                return "<input name=\"" + checkboxName
                        + "\" id='chk_" + rowcount
                        + "' type=\"checkbox\" value=\""
                        + v
                        + "\">";
            });

            chkbox.setTitle(" ");
            chkbox.setFilterable(false);
            chkbox.setSortable(false);
            chkbox.setEditable(false);
            return tableModel.render();
        }
    }

    protected void exportRender(TableModel tableModel) {
        if (TableModel.EXCEL.equals(tableModel.getExportType())) {
            tableModel.setExportFileName(tableCaption + ".xls");
        }

        log.debug("Exporting - " + tableModel.getExportType() + ": " + tableCaption + ".xls");
        tableModel.render();
    }


    public String getTableCaption(String tableName) {
        if (tableCaption == null) {
            ResourceBundle bundle = ResourceBundle.getBundle(TABLE_I18N);
            try {
                tableCaption = bundle.getString(tableName);
                return tableCaption;
            } catch (Exception e) {
                mLog.warn(e.getMessage());
                return tableName;
            }
        }
        return tableCaption;
    }

    public void setTableCaption(String tableCaption) {
        this.tableCaption = tableCaption;
    }

    protected abstract String getUniquePropertyName();


    /**
     * user must override this method for use like below:
     * if (changedValue.equals("foo")) {
     * worksheetColumn.setErrorKey("foo.error");
     * } else {
     * worksheetColumn.removeError();
     * }
     */
    protected void validateColumn(WorksheetColumn worksheetColumn,
                                  String changedValue) {
        mLog.debug(" NO custom validateColumn method is implemented");
    }

    public String handleRequestInternal(HttpServletRequest request,
                                        HttpServletResponse response) throws SQLException {
        String mv = successView;
        String html = render(request, response);
        if (html == null) {
            return null; // an export
        } else {
            request.setAttribute(this.id, html); // Set the Html in the
            // request for the JSP.
        }
        return mv;
    }

    /**
     * A very custom way to filter the items. The PresidentFilter acts as a
     * command for the Hibernate criteria object. There are probably many ways
     * to do this, but this is the most flexible way I have found. The point is
     * you need to somehow take the Limit information and filter the rows.
     *
     * @param limit The Limit to use.
     */
    public static SqlFilter getFilter(Limit limit) {
        SqlFilter sqlFilter = new SqlFilter();
        FilterSet filterSet = limit.getFilterSet();
        java.util.Collection<Filter> filters = filterSet.getFilters();

        for (Filter filter : filters) {
            String property = filter.getProperty();
            String value = filter.getValue();
            sqlFilter.addFilter(property, value);
        }
        return sqlFilter;
    }

    public static SqlFilter getFilter(Limit limit, String tableAlias) throws ParseException {
        SqlFilter sqlFilter = new SqlFilter();
        FilterSet filterSet = limit.getFilterSet();
        java.util.Collection<Filter> filters = filterSet.getFilters();

        for (Filter filter : filters) {
            String property = filter.getProperty();
            String value = filter.getValue();
            String prop = tableAlias + "." + property;
            if(TimeSpan.isTimeSpan(value)) {
                TimeSpan ts = TimeSpan.build(value);
                sqlFilter.addFilter(prop, ts);
            }else {
                sqlFilter.addFilter(prop, value);
            }
        }
        return sqlFilter;
    }

    /**
     * A very custom way to sort the items. The PresidentSort acts as a command
     * for the Hibernate criteria object. There are probably many ways to do
     * this, but this is the most flexible way I have found. The point is you
     * need to somehow take the Limit information and sort the rows.
     *
     * @param limit The Limit to use.
     */
    public static SqlSort getSort(Limit limit) {
        SqlSort sqlSort = new SqlSort();
        SortSet sortSet = limit.getSortSet();
        java.util.Collection<Sort> sorts = sortSet.getSorts();
        for (Sort sort : sorts) {
            String property = sort.getProperty();
            String order = sort.getOrder().toParam();
            sqlSort.addSort(property, order);
        }
        return sqlSort;
    }

    public void setSuccessView(String successView) {
        this.successView = successView;
    }

    protected Object convert(String value, Class<?> clazz) {
        if (clazz == ClobString.class) {
            return new ClobString(value);
        }

        return ConvertUtils.convert(value, clazz);

    }

    protected void performAction(String action, TableModel tableModel, V[] keys)
            throws SQLException {

        if ("save_worksheet".equals(action)) {
            tableModel.saveWorksheet(this);
        } else if (CustomToolbar.EDIT.equals(action)) {

        } else if ("add_worksheet_row".equals(action)
                || CustomToolbar.INSERT.equals(action)) {
            service.insertDummy();
        } else if (CustomToolbar.DELETE.equals(action)) {
            deleteSelected(keys);
        } else if (CustomToolbar.COPY.equals(action)) {
            copySelected(keys);
        } else if (CustomToolbar.RUN.equals(action)) {
            onRun(tableModel, keys);
        }
    }

    protected void deleteSelected(V[] keys) throws SQLException {
        if (keys == null) {
            return;
        }
        for (int i = 0; i < keys.length; i++) {
            if (!service.deleteByKey(keys[i])) {
                mLog.warn("fail to delete key=" + keys[i]);
            }
        }
    }

    protected void copySelected(V[] parameterValues) throws SQLException {
        if (parameterValues == null)
            return;
        java.util.ArrayList<String> list = new java.util.ArrayList<String>();
        for (V parameterValue : parameterValues) {
            list.add(parameterValue.toString());
        }
        Map<String, T> infos = service.selectByIds(getUniquePropertyName(),
                list);
        for (T info : infos.values()) {
            if (!service.insertAutoNewKey(info)) {
                mLog.warn("fail to insert info=" + info.toString());
            }
        }
    }

    /**
     * An example of how to save the worksheet.
     */
    @Override
    public void saveWorksheet(Worksheet worksheet) {
        if (!worksheet.isSaving() || !worksheet.hasChanges()) {
            return;
        }
        String uniquePropertyName = WorksheetUtils
                .getUniquePropertyName(worksheet);
        List<String> uniquePropertyValues = WorksheetUtils
                .getUniquePropertyValues(worksheet);
        final Map<String, T> infos;
        try {
            infos = service.selectByIds(uniquePropertyName,
                    uniquePropertyValues);
        } catch (SQLException e1) {
            mLog.error(e1);
            return;
        }
        worksheet.processRows(worksheetRow -> {
            Collection<WorksheetColumn> columns = worksheetRow.getColumns();
            for (WorksheetColumn worksheetColumn : columns) {
                String changedValue = worksheetColumn.getChangedValue();
                validateColumn(worksheetColumn, changedValue);
                if (worksheetColumn.hasError()) {
                    continue;
                }
                String uniqueValue = worksheetRow.getUniqueProperty()
                        .getValue();
                T info = infos.get(uniqueValue);
                boolean isNew = false;
                if (info == null) {
                    info = service.create();
                    isNew = true;
                }
                String property = worksheetColumn.getProperty();
                try {
                    if (worksheetColumn.getProperty().equals("selected")) {
                        if (changedValue
                                .equals(CheckboxWorksheetEditor.CHECKED)) {
                            PropertyUtils.setProperty(info, property, "y");
                        } else {
                            PropertyUtils.setProperty(info, property, "n");
                        }
                    } else {
                        Object value = convert(changedValue, PropertyUtils
                                .getPropertyType(info, property));

                        PropertyUtils.setProperty(info, property, value);
                    }
                } catch (Exception ex) {
                    mLog.error(ex);
                    throw new JMesaException(
                            "Not able to set the property [" + property
                                    + "] when saving worksheet.");
                }
                try {
                    boolean res = isNew ? service.insert(info) : service.updateByKey(info);
                    if (!res) {
                        mLog.warn("fail to update info=" + info.toString());
                    }
                } catch (SQLException e) {
                    mLog.error(e);
                }
            }
        });
    }


    protected void setDataAndLimitVariables(TableModel tableModel) {
        tableModel.setItems(new ItemsProvider<T>(this.getUniquePropertyName(), service));
    }

    public Map<String, String> getColsTemplate() {
        return colsTemplate;
    }

    public void setColsTemplate(Map<String, String> colsTemplate) {
        this.colsTemplate = colsTemplate;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getContext(HttpServletRequest request) {
        if (request == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> context = new HashMap<String, Object>();
        context.putAll(request.getParameterMap());
        Enumeration<?> en = request.getAttributeNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            context.put(key, request.getAttribute(key));
        }
        if (request.getSession() != null) {
            HttpSession session = request.getSession();
            Enumeration<?> sen = session.getAttributeNames();
            while (sen.hasMoreElements()) {
                String key = (String) sen.nextElement();
                context.put(key, session.getAttribute(key));
            }
        }
        return context;
    }

    protected void renderCell(Column col, Map<String, ?> context) {

        String var = VAR_NAME;

        String template = getColsTemplate() == null ? "" : getColsTemplate().get(col.getProperty());

        if (StringUtils.isBlank(template)) {
            return;
        }

        template = template.replaceAll("\\%\\{(.+?)\\}", "\\$\\{$1\\}");

        log.debug("after replace " + var + ":" + template);
        CellEditor cellEditor = new ElExpressionExCellEditor(var, template, context);

        col.setCellEditor(cellEditor);
    }
}