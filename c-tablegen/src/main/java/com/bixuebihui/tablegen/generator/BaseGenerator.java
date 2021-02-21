package com.bixuebihui.tablegen.generator;


import com.bixuebihui.dbcon.DatabaseConfig;
import com.bixuebihui.jdbc.IDbHelper;
import com.bixuebihui.tablegen.NameUtils;
import com.bixuebihui.tablegen.ProjectConfig;
import com.bixuebihui.tablegen.TableGen;
import com.bixuebihui.tablegen.TableUtils;
import com.bixuebihui.tablegen.entry.TableInfo;
import com.bixuebihui.tablegen.entry.TableSetInfo;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.bixuebihui.tablegen.NameUtils.firstUp;
import static com.bixuebihui.tablegen.NameUtils.getConfigBaseDir;
import static com.bixuebihui.tablegen.TableGenConfig.PROPERTIES_FILENAME;

/**
 * @author xwx
 */
public abstract class BaseGenerator {
    protected static final String TEMPLATE_ROOT = "/templates";
    private static final Log LOG = LogFactory.getLog(BaseGenerator.class);

    ProjectConfig config;
    DatabaseConfig dbConfig;
    TableSetInfo setInfo;

    public BaseGenerator() {
        config = new ProjectConfig();
        dbConfig = new DatabaseConfig();
        setInfo = new TableSetInfo();
    }

    public static boolean isNotEmpty(Collection<?> col) {
        return !CollectionUtils.isEmpty(col);
    }

    public void init(ProjectConfig config, DatabaseConfig dbConfig, TableSetInfo setInfo) {
        this.config = config;
        this.dbConfig = dbConfig;
        this.setInfo = setInfo;
    }

    public synchronized void readDb(DatabaseConfig dbConfig) throws SQLException, InstantiationException, IOException, IllegalAccessException {

        IDbHelper helper = TableGen.getDbHelper(dbConfig);

        DatabaseMetaData metaData = helper.getConnection().getMetaData();
        setInfo.getTableData(config, helper, metaData);
    }

    abstract String getTargetFileName(String tableName);

    abstract String getTemplateFileName();

    public String getPojoClassName(String tableName) {
        String classname = setInfo.tableName2ClassName(tableName);
        if (tableName.equals(classname)) {
            return config.getPrefix() + firstUp(tableName);
        } else {
            return config.getPrefix() + classname;
        }
    }

    abstract String getClassName(String tableName);

    public synchronized void init(String filename) {
        String propertiesFilename = filename != null ? filename : PROPERTIES_FILENAME;

        try (FileInputStream fis = new FileInputStream(propertiesFilename)) {
            Properties props = new Properties();

            props.load(fis);

            dbConfig.readDbConfig(props);

            config.readFrom(props, getConfigBaseDir(propertiesFilename));

            readDb(dbConfig);

        } catch (IOException | SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected String getInterface(String tableName) {
        return setInfo.getInterface(tableName, config);
    }

    protected String getExtendsClasses(String tableName) {
        return "";
    }

    protected void additionalSetting(Handlebars handlebars) {
    }

    public String generate(String tableName) throws IOException {
        Handlebars handlebars = getHandlebars();

        Template template = handlebars.compile(File.separator + getTemplateFileName());

        Map<String, Object> v = getContextMap(tableName);
        return template.apply(Context.newBuilder(v).resolver(
                MethodValueResolver.INSTANCE,
                JavaBeanValueResolver.INSTANCE,
                FieldValueResolver.INSTANCE,
                MapValueResolver.INSTANCE
        ).build());
    }

    protected Map<String, Object> getContextMap(String tableName) {
        Map<String, Object> v = new HashMap<>(20);
        v.put("tableName", tableName);
        v.put("tableInfo", setInfo.getTableInfos().get(tableName));
        v.put("fields", setInfo.getTableCols(tableName));
        v.put("keys", setInfo.getTableKeys(tableName));
        v.put("importKeys", setInfo.getTableImportedKeys(tableName));
        v.put("exportKeys", setInfo.getTableExportedKeys(tableName));
        v.put("pojoClassName", this.getPojoClassName(tableName));
        v.put("className", this.getClassName(tableName));
        v.put("interface", this.getInterface(tableName));
        v.put("extends", this.getExtendsClasses(tableName));
        v.put("hasKey", isNotEmpty(this.setInfo.getTableKeys(tableName)));
        v.put("setInfo", setInfo);
        v.put("config", config);
        return v;
    }

    protected Handlebars getHandlebars() {
        TemplateLoader loader = new ClassPathTemplateLoader();
        loader.setPrefix(TEMPLATE_ROOT);
        loader.setSuffix(".hbs");

        Handlebars handlebars = new Handlebars(loader);
        handlebars.registerHelper("firstUp", (name, options) -> NameUtils.firstUp((String) name));
        handlebars.registerHelper("getPojoClassName", (tableName, options) -> this.getPojoClassName((String) tableName));
        handlebars.registerHelper("constantName", (name, options) -> NameUtils.columnNameToConstantName((String) name));
        handlebars.registerHelper("firstLow", (name, options) -> NameUtils.firstLow((String) name));
        handlebars.registerHelper("join", (items, options) -> StringUtils.join((Object[]) items, options.param(0)));
        handlebars.registerHelper("typeDefaultValue", (typeName, options) -> TableGen.defaultTypeValue().get(typeName));
        handlebars.registerHelpers(ConditionalHelpers.class);

        //copied from AssignHelper
        handlebars.registerHelper("let", (String variableName, Options options) -> {
            CharSequence finalValue = options.apply(options.fn);
            options.context.data(variableName, finalValue.toString().trim());
            return null;
        });

        additionalSetting(handlebars);
        return handlebars;
    }

}
