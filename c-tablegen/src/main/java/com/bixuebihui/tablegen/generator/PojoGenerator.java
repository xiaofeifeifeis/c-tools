package com.bixuebihui.tablegen.generator;

import com.bixuebihui.generated.tablegen.pojo.T_metatable;
import com.bixuebihui.tablegen.TableUtils;
import com.bixuebihui.tablegen.entry.ColumnData;
import com.bixuebihui.tablegen.entry.TableSetInfo;
import com.github.jknack.handlebars.Handlebars;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

import static com.bixuebihui.tablegen.NameUtils.firstUp;

/**
 * @author xwx
 */
public class PojoGenerator extends BaseGenerator{
    private static final Log LOG = LogFactory.getLog(PojoGenerator.class);

    public static String getExtendsClasses(TableSetInfo setInfo, String tableName) {
        if (setInfo.getTableDataExt() != null) {
            T_metatable tab = setInfo.getTableDataExt().get(tableName);
            if (tab != null) {
                StringBuilder sb = new StringBuilder();
                if (StringUtils.isNotBlank(tab.getExtrasuperclasses())) {
                    sb.append(", extends ").append(tab.getExtrasuperclasses());
                }
                return sb.toString();
            }
        }
        return "";
    }

    public static void main(String[] args) throws IOException {
        String res = new PojoGenerator().generate();
        System.out.println(res);
    }

    @Override
    public String getTargetFileName(String tableName) {
        String baseDir = config.getBaseDir();

        return baseDir + File.separator + "pojo" + File.separator + getClassName(tableName)
                + ".java";
    }

    @Override
    String getTemplateFileName() {
        return "pojo.java";
    }

    @Override
    public String getClassName(String tableName) {
        String classname = setInfo.tableName2ClassName(tableName);
        if (tableName.equals(classname)) {
            return config.getPrefix() + firstUp(tableName);
        } else {
            return config.getPrefix() + classname;
        }
    }

    @Override
    protected void additionalSetting(Handlebars handlebars) {
        super.additionalSetting(handlebars);
        // usage: {{columnAnnotation aColumnData tableName}}
        handlebars.registerHelper("columnAnnotation", (col, options) -> TableUtils.getColumnAnnotation(config, setInfo, options.param(0),(ColumnData) col));

        // usage: {{columnDescription aColumnData tableName}}
        handlebars.registerHelper("columnDescription", (col, options) -> TableUtils.getColumnDescription(config, setInfo.getColumnsExtInfo(options.param(0)), options.param(0),((ColumnData) col).getName()));
    }

    @Override
    protected String getExtendsClasses(String tableName) {
      return getExtendsClasses(setInfo, tableName);
    }

}
