package com.github.bookong.zest.core.testcase;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import com.github.bookong.zest.core.xml.data.Data;
import com.github.bookong.zest.core.xml.data.DataSource;
import com.github.bookong.zest.core.xml.data.ParamField;
import com.github.bookong.zest.core.xml.data.TestParam;
import com.github.bookong.zest.exceptions.LoadTestCaseFileException;
import com.github.bookong.zest.util.LoadTestCaseUtil;
import com.github.bookong.zest.util.Messages;
import com.github.bookong.zest.util.ZestReflectHelper;

/**
 * @author jiangxu
 */
public class TestCaseData {

    /** 测试数据文件名称 */
    private String                                         fileName;
    /** 对于日期类型，在插入数据库时是否需要做偏移处理 */
    private boolean                                        transferTime = false;
    /** 如果日期需要偏移处理，当前时间与测试用例上描述的时间相差多少毫秒 */
    private long                                           currDbTimeDiff;
    /** 描述 */
    private String                                         description;
    /** 测试参数 */
    private ZestTestParam                                  testParam;
    /** 数据源描述 */
    private List<TestCaseDataSource>                       dataSources  = new ArrayList<>();
    /** 开始进行测试的时间 */
    private long                                           startTime;
    /** 测试结束的时间 */
    private long                                           endTime;
    /** 关系型数据库的 SqlType，第一层 key 是 TestDataSource 的 id, 第二层的 key 是表名，第三层的 key 是列名 */
    private Map<String, Map<String, Map<String, Integer>>> rmdbSqlTypes = new HashMap<>();

    /**
     * 用 XML 数据初始化对象
     * 
     * @param data
     * @throws LoadTestCaseFileException
     */
    public void load(Data xmlData) throws LoadTestCaseFileException {
        description = xmlData.getDescription();
        transferTime = StringUtils.isNotBlank(xmlData.getCurrDbTime());
        if (transferTime) {
            Date currDbTime = LoadTestCaseUtil.parseDate(xmlData.getCurrDbTime());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            currDbTimeDiff = cal.getTimeInMillis() - currDbTime.getTime();
        }

        load(xmlData.getTestParam());
        if (xmlData.getDataSources() != null && xmlData.getDataSources().getDataSource() != null) {
            for (DataSource xmlDataSource : xmlData.getDataSources().getDataSource()) {
                dataSources.add(new TestCaseDataSource(this, xmlDataSource));
            }
        }
    }

    /** 比较时间（考虑数据偏移情况) */
    public void assertDateEquals(String msg, String dateFormat, String expect, String actual) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        try {
            if (StringUtils.isBlank(expect) && StringUtils.isBlank(actual)) {
                return;
            }
            assertDateEquals(msg, sdf.parse(expect), sdf.parse(actual));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /** 比较时间（考虑数据偏移情况) */
    public void assertDateEquals(String msg, Date expect, Date actual) {

        long expectMillisecond = expect.getTime();
        if (isTransferTime()) {
            expectMillisecond += getCurrDbTimeDiff();
        }

        Assert.assertEquals(msg, expectMillisecond, actual.getTime());
    }

    @SuppressWarnings({ "unchecked" })
    private void load(TestParam xmlTestParam) throws LoadTestCaseFileException {

        for (ParamField xmlParamField : xmlTestParam.getParamField()) {
            try {
                List<TestParamField> paramFields = new ArrayList<>();
                for (String str : xmlParamField.getPath().trim().split("/", -1)) {
                    paramFields.add(new TestParamField(str));
                }

                TestParamInfo info = createTestParamInfo(getTestParam(), paramFields);
                if (xmlParamField.isNull()) {
                    ZestReflectHelper.setValueByFieldName(info.getObj(), info.getField().getName(), null);
                } else {
                    Object value = LoadTestCaseUtil.loadXmlFieldValue(info, xmlParamField.getValue());
                    if (Map.class.isAssignableFrom(info.getFieldClass())) {
                        info.getMap().put(info.getTestParamField().getSubscript(), value);
                    } else if (List.class.isAssignableFrom(info.getFieldClass())) {
                        info.getList().add(value);
                    } else {
                        ZestReflectHelper.setValueByFieldName(info.getObj(), info.getField().getName(), value);
                    }
                }
            } catch (Exception e) {
                throw new LoadTestCaseFileException(Messages.getString("testCaseData.failToLoadPath", xmlParamField.getPath()), e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private TestParamInfo createTestParamInfo(Object parent, List<TestParamField> paramFields) {
        TestParamInfo info = new TestParamInfo();
        Object obj = parent;

        for (int i = 0; i < paramFields.size(); i++) {
            TestParamField testParamField = paramFields.get(i);
            boolean notLastOne = (i < paramFields.size() - 1);

            info.setTestParamField(testParamField);
            info.setField(ZestReflectHelper.getFieldByFieldName(obj, testParamField.getFieldName()));
            info.setObj(obj);
            if (info.getField() == null) {
                throw new RuntimeException(Messages.getString("testCaseData.canNotFindField", testParamField.getFieldName(), obj.getClass().getName()));
            }

            Class<?> fieldClass = info.getField().getType();
            info.setFieldClass(fieldClass);

            if (testParamField.isMap() && !Map.class.isAssignableFrom(fieldClass)) {
                // xml 文件与实际对象不一致
                throw new RuntimeException(Messages.getString("testCaseData.notClassMapObject", testParamField.getFieldName()));
            }

            obj = ZestReflectHelper.getValueByFieldName(obj, info.getField().getName());

            // 由于 Map 和 List 容器只支持包含基本类型，所以他们必须出现在最后一层的属性中
            if (Map.class.isAssignableFrom(fieldClass)) {
                if (notLastOne) {
                    throw new RuntimeException(Messages.getString("testCaseData.mapMustLastField", testParamField.getFieldName()));
                }
                info.setMap((Map) obj);
            }
            if (List.class.isAssignableFrom(fieldClass)) {
                if (notLastOne) {
                    throw new RuntimeException(Messages.getString("testCaseData.listMustLastField", testParamField.getFieldName()));
                }
                info.setList((List) obj);
            }
        }

        return info;
    }

    /**
     * 获取指定数据源下指定表下各个列的 SqlType
     * 
     * @param testDataSourceId 数据源ID
     * @param tableName 表名
     * @return 返回一个 map，key 为列名，value 是 SqlType
     */
    public Map<String, Integer> getRmdbTableColSqlTypes(String testDataSourceId, String tableName) {
        return Optional.of(rmdbSqlTypes).map(o -> o.get(testDataSourceId.toLowerCase())).map(o -> o.get(tableName.toLowerCase())).orElseGet(HashMap::new);
    }

    /**
     * 设定指定数据源下指定表的 SqlType
     * 
     * @param testDataSourceId 数据源ID
     * @param tableName 表名
     * @param colName 列名
     * @param sqlType 关系型数据库的 SqlType
     */
    public void putRmdbTableColSqlTypes(String testDataSourceId, String tableName, String colName, Integer sqlType) {
        testDataSourceId = testDataSourceId.toLowerCase();
        tableName = tableName.toLowerCase();
        colName = colName.toLowerCase();
        Map<String, Map<String, Integer>> map1 = rmdbSqlTypes.get(testDataSourceId);
        if (map1 == null) {
            map1 = new HashMap<>();
            rmdbSqlTypes.put(testDataSourceId, map1);
        }

        Map<String, Integer> map2 = map1.get(tableName);
        if (map2 == null) {
            map2 = new HashMap<>();
            map1.put(tableName, map2);
        }

        map2.put(colName.toLowerCase(), sqlType);
    }

    @SuppressWarnings("rawtypes")
    public class TestParamInfo {

        private Field          field;
        private Class<?>       fieldClass;
        private Object         obj;
        private TestParamField testParamField;
        private Map            map;
        private List           list;

        public Field getField() {
            return field;
        }

        public void setField(Field field) {
            this.field = field;
        }

        public Object getObj() {
            return obj;
        }

        public void setObj(Object obj) {
            this.obj = obj;
        }

        public TestParamField getTestParamField() {
            return testParamField;
        }

        public void setTestParamField(TestParamField testParamField) {
            this.testParamField = testParamField;
        }

        public Class<?> getFieldClass() {
            return fieldClass;
        }

        public void setFieldClass(Class<?> fieldClass) {
            this.fieldClass = fieldClass;
        }

        public Map getMap() {
            return map;
        }

        public void setMap(Map map) {
            this.map = map;
        }

        public List getList() {
            return list;
        }

        public void setList(List list) {
            this.list = list;
        }

    }

    public class TestParamField {

        private String fieldName;
        private String subscript;

        public TestParamField(String str){
            fieldName = str;
            subscript = StringUtils.EMPTY; // 有下标就表示是 Map
            int pos = str.indexOf(':'); // $NON-NLS-1$
            if (pos > 0) {
                fieldName = str.substring(0, pos);
                subscript = str.substring(pos + 1);
            }
        }

        public boolean isMap() {
            return StringUtils.isNotBlank(subscript);
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getSubscript() {
            return subscript;
        }

        public void setSubscript(String subscript) {
            this.subscript = subscript;
        }

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDescription() {
        return description;
    }

    public ZestTestParam getTestParam() {
        return testParam;
    }

    public void setTestParam(ZestTestParam testParam) {
        this.testParam = testParam;
    }

    public List<TestCaseDataSource> getDataSources() {
        return dataSources;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isTransferTime() {
        return transferTime;
    }

    public long getCurrDbTimeDiff() {
        return currDbTimeDiff;
    }
}
