package io.gallery.util;

import io.gallery.util.bean.ExportType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Utils {
    private static final Log logger = LogFactory.getLog(Utils.class);

    /**
     * 获得request对象
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * 通过正则获取内容
     *
     * @param input   输入
     * @param pattern 正则
     * @return String
     */
    public static String getByPattern(String input, String pattern) {
        String result = "";
        Matcher matcher = Pattern.compile(pattern).matcher(input);
        while (matcher.find()) {
            result = matcher.group(0);
            break;
        }
        return result;
    }

    /**
     * 字符串判空
     *
     * @param input 输入
     * @return Boolean
     */
    public static Boolean isNull(String input) {
        return null == input || "".equals(input);
    }

    /**
     * 字符串判空
     *
     * @param input 输入
     * @return Boolean
     */
    public static Boolean isNotNull(String input) {
        return !isNull(input);
    }

    /**
     * 截取字符串
     *
     * @param input 输入
     * @param start 开始
     * @param end   结束
     * @return String
     */
    public static String subString(String input, int start, int end) {
        if (!isNull(input) && input.length() >= end) {
            input = input.substring(start, end);
        } else if (!isNull(input) && input.length() < end) {
            input = input.substring(start);
        } else {
            return null;
        }
        return input;

    }

    /**
     * 对象转Map
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        if (obj instanceof Map) {
            Map<String, Object> temp = new HashMap<>((Map<String, Object>) obj);
            map = new HashMap<>((Map<String, Object>) obj);
            for (String s : temp.keySet()) {
                Object o = temp.get(s);
                if (o == null)
                    map.remove(s);
            }
        } else {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor property : propertyDescriptors) {
                    String key = property.getName();
                    if (key.compareToIgnoreCase("class") == 0)
                        continue;
                    Method getter = property.getReadMethod();
                    Object value = getter != null ? getter.invoke(obj) : null;
                    if (value != null)
                        map.put(key, value);
                }
            } catch (Exception e) {
                logger.error("objectToMap error:" + e.getMessage());
            }
        }
        return map;
    }

    /**
     * Map转Bean
     *
     * @param map
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> clazz) {
        T bean = null;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            bean = clazz.newInstance();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (map.containsKey(key))
                    setParamter(map, bean, key, property);
            }
        } catch (Exception e) {
            logger.error("mapToBean error:" + e.getMessage());
        }
        return bean;
    }

    /**
     * Map转Bean忽略大小写
     *
     * @param map
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T mapToBeanIngnoreCase(Map<String, Object> map, Class<T> clazz) {
        T bean = null;
        if (map != null && clazz != null) {
            if (!clazz.getSimpleName().contains("Map")) {
                try {
                    BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                    PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                    bean = clazz.newInstance();
                    for (String mapKey : map.keySet()) {
                        for (PropertyDescriptor property : propertyDescriptors) {
                            String key = property.getName().toLowerCase();
                            if (mapKey.equalsIgnoreCase(key)) {
                                setParamter(map, bean, mapKey, property);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("mapToBeanIngnoreCase error:" + e.getMessage(), e.getCause());
                }
            } else {
                bean = (T) map;
            }
        } else if (map == null && clazz != null) {
            try {
                bean = clazz.newInstance();
            } catch (Exception e) {
                logger.error("mapToBeanIngnoreCase error:" + e.getMessage(), e.getCause());
            }
        }
        return bean;
    }

    private static <T> void setParamter(Map<String, Object> map, T bean, String mapKey, PropertyDescriptor property) {
        Object value = map.get(mapKey);
        Method setter = property.getWriteMethod();
        Class<?>[] parameterTypes = setter.getParameterTypes();
        try {
            if (parameterTypes.length > 0) {
                if (parameterTypes[0].getName().equalsIgnoreCase(Integer.class.getName())) {
                    setter.invoke(bean, new Integer(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(BigDecimal.class.getName())) {
                    setter.invoke(bean, new BigDecimal(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Long.class.getName())) {
                    setter.invoke(bean, new Long(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Double.class.getName())) {
                    setter.invoke(bean, new Double(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Float.class.getName())) {
                    setter.invoke(bean, new Float(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Short.class.getName())) {
                    setter.invoke(bean, new Short(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Byte.class.getName())) {
                    setter.invoke(bean, new Byte(value.toString()));
                } else if (parameterTypes[0].getName().equalsIgnoreCase(Boolean.class.getName()) || parameterTypes[0].getName().equalsIgnoreCase(boolean.class.getName())) {
                    if (value.toString().equalsIgnoreCase("0") || value.toString().equalsIgnoreCase("false")) {
                        setter.invoke(bean, Boolean.FALSE);
                    } else if (value.toString().equalsIgnoreCase("1") || value.toString().equalsIgnoreCase("true")) {
                        setter.invoke(bean, Boolean.TRUE);
                    } else {
                        setter.invoke(bean, new Boolean(value.toString()));
                    }
                } else if (parameterTypes[0].isEnum()) {
                    Object[] enumConstants = parameterTypes[0].getEnumConstants();
                    for (Object o : enumConstants) {
                        if (o.toString().equalsIgnoreCase(value.toString())) {
                            setter.invoke(bean, o);
                        }
                    }
                } else {
                    setter.invoke(bean, value);
                }
            }
        } catch (Exception e) {
            try {
                setter.invoke(bean, value.toString());
            } catch (Exception exception) {
                logger.error("value: [" + value + "] set error:" + e.getMessage(), e.getCause());
            }
        }
    }

    /**
     * Bean字段List
     *
     * @param clazz
     * @return
     */
    public static List<String> classKeyToList(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        List<String> list = new ArrayList<>();
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            list.add(field.getName());
        }
        return list;
    }

    /**
     * 合并对象至Map
     *
     * @param source
     * @param target
     * @return
     */
    public static Map merge(Object source, Map target) {
        if (target != null) {
            target.putAll(objectToMap(source));
        }
        return target;
    }

    /**
     * 获取参数Map
     *
     * @return
     */
    public static Map<String, Object> getParamMap() {
        return getParamObjectMap(getRequest());
    }

    private static void dealPageInfo(Map map) {
        try {
            Object start = map.get("start");
            Object length = map.get("length");
            if (start instanceof String) {
                map.put("start", Integer.valueOf((String) start));
            }
            if (length instanceof String) {
                map.put("length", Integer.valueOf((String) length));
            }
        } catch (Exception e) {
            logger.error("分页信息转换失败");
        }
    }

    private static Map<String, Object> getParamObjectMap(HttpServletRequest request) {
        Map<String, String[]> properties = request.getParameterMap();
        Map<String, Object> result = new HashMap<>();
        Iterator<Map.Entry<String, String[]>> iter = properties.entrySet().iterator();
        String name;
        String value = "";
        while (iter.hasNext()) {
            Map.Entry<String, String[]> entry = iter.next();
            name = entry.getKey();
            Object valueObj = entry.getValue();
            if (null == valueObj) {
                value = "";
            } else {
                String[] values = (String[]) valueObj;
                for (String s : values) {
                    value = s + ",";
                }
                value = value.substring(0, value.length() - 1);
            }
            result.put(name, value);
        }
        dealPageInfo(result);
        return result;
    }

    /**
     * 获取参数Map
     *
     * @return
     */
    public static Map<String, String> getParamStringMap() {
        return getParamStringMap(getRequest());
    }

    public static Map<String, String> getParamStringMap(HttpServletRequest request) {
        Map<String, String[]> properties = request.getParameterMap();
        Map<String, String> returnMap = new HashMap<>();
        String name;
        String value = "";
        for (Map.Entry<String, String[]> entry : properties.entrySet()) {
            name = entry.getKey();
            String[] values = entry.getValue();
            if (null == values) {
                value = "";
            } else if (values.length > 1) {
                for (String s : values) {
                    value = s + ",";
                }
                value = value.substring(0, value.length() - 1);
            } else {
                value = values[0];
            }
            returnMap.put(name, value);

        }
        return returnMap;
    }

    /**
     * 首字母转大写
     *
     * @param input 输入
     * @return String
     */
    public static String toUpperCaseFirst(String input) {
        if (Character.isUpperCase(input.charAt(0))) {
            return input;
        } else {
            return Character.toUpperCase(input.charAt(0)) + input.substring(1);
        }
    }

    /**
     * 首字母转小写
     *
     * @param input 输入
     * @return String
     */
    public static String toLowerCaseFirst(String input) {
        if (Character.isLowerCase(input.charAt(0))) {
            return input;
        } else {
            return Character.toLowerCase(input.charAt(0)) + input.substring(1);
        }
    }

    /**
     * 字符串md5加密
     *
     * @param input 输入
     * @return String
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static String getMD5(String input) throws NoSuchAlgorithmException {
        if (input != null) {
            MessageDigest md = MessageDigest.getInstance("MD5");// 生成一个MD5加密计算摘要
            md.update(input.getBytes()); // 计算md5函数
            return new BigInteger(1, md.digest()).toString(16);// digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符 BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
        } else {
            return null;
        }
    }

    /**
     * 解压缩字节
     *
     * @param input 输入
     * @return String
     */
    public static String unCompress(byte[] input) {
        return unCompress(input, "UTF-8");
    }

    /**
     * 解压缩字节
     *
     * @param input    输入
     * @param encoding 编码
     * @return String
     */
    public static String unCompress(byte[] input, String encoding) {
        if (input == null || input.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = ungzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            return out.toString(encoding);
        } catch (IOException e) {
            logger.error("unCompress失败：" + e.getMessage(), e.getCause());
        }
        return null;
    }

    /**
     * 压缩字符串
     *
     * @param input 输入
     * @return String
     */
    public static byte[] compress(String input) {
        return compress(input, "UTF-8");
    }

    /**
     * 压缩字符串
     *
     * @param input    输入
     * @param encoding 编码
     * @return String
     */
    public static byte[] compress(String input, String encoding) {
        if (input == null || input.length() == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(input.getBytes(encoding));
            gzip.close();
        } catch (Exception e) {
            logger.error("compress失败：" + e.getMessage(), e.getCause());
        }
        return out.toByteArray();
    }

    /**
     * 从字段名和字段类型例表中获取字段名
     *
     * @param columnNameWithType
     * @return
     */
    public static String getColumnName(String columnNameWithType) {
        if (isNotNull(columnNameWithType) && columnNameWithType.contains("::")) {
            return columnNameWithType.split("::")[0];
        }
        return columnNameWithType;
    }

    /**
     * 从字段名和字段类型例表中获取字段类型
     *
     * @param columnNameWithType
     * @return
     */
    public static String getColumnType(String columnNameWithType) {
        if (isNotNull(columnNameWithType) && columnNameWithType.contains("::")) {
            return columnNameWithType.split("::")[1];
        }
        return columnNameWithType;
    }

    /**
     * 处理大文本数据
     *
     * @param map
     */
    public static void dealMegaText(Map map) {
        if (map != null)
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) map).entrySet()) {
                if (entry.getValue() instanceof Clob) {
                    Clob clob = (Clob) entry.getValue();
                    try {
                        map.put(entry.getKey(), clob.getSubString(1, (int) clob.length()));
                    } catch (SQLException e) {
                        logger.error("clob convert error: " + e.getMessage(), e);
                    }
                }
            }
    }

    /**
     * Map的所有键大小写转换
     *
     * @param map
     * @param lowerCase
     * @return
     */
    public static Map mapKeyCase(Map map, Boolean lowerCase) {
        Map<String, Object> result = new HashMap<>();
        if (map != null) {
            Set<String> keySet = map.keySet();
            for (String key : keySet) {
                if (Boolean.FALSE.equals(lowerCase)) {
                    result.put(key.toUpperCase(), map.get(key));
                } else if (Boolean.TRUE.equals(lowerCase)) {
                    result.put(key.toLowerCase(), map.get(key));
                }
            }
            map = result;
        }
        return map;
    }

    /**
     * 构造树结构
     *
     * @param list       原始数据
     * @param treeColumn 递归字段名
     * @param treeColumn 递归父字段名
     * @param treePlain  是否是展示原始树
     * @return
     */
    public static List dealTree(List list, String column, String treeColumn, boolean treePlain) {
        Integer level = 0;
        List firstLevel = (List) list.stream().filter(o -> {
            Object treeLevel = objectToMap(o).get("tree_level");
            if (treeLevel instanceof Integer)
                return level.equals(treeLevel);
            else if (treeLevel instanceof Double)
                return (Double) treeLevel - level == 0;
            return false;
        }).collect(Collectors.toList());
        List tree = new ArrayList();
        list.removeAll(firstLevel);
        for (Object o : firstLevel) {
            Map<String, Object> map = objectToMap(o);
            Object id = map.get(column);
            Map<String, Object> leaf = new HashMap<>();
            if (!treePlain) {//不显示原始树
                Object parentId = map.get(treeColumn);
                leaf.put("id", id);
                leaf.put("pid", parentId);
                leaf.put("data", o);
            } else
                leaf = map;
            leaf.put("children", dealLeaf(list, column, treeColumn, id, treePlain));
            tree.add(leaf);
        }
        return tree;
    }

    /**
     * 构造叶子节点
     *
     * @param list       原始数据
     * @param treeColumn 递归字段名
     * @param treeColumn 递归父字段名
     * @param parentId   父级节点ID
     * @return
     */
    private static List dealLeaf(List list, String column, String treeColumn, Object parentId, boolean treePlain) {
        if (parentId == null || list.size() == 0) {
            return null;
        }
        List result = null;
        List children = (List) list.stream().filter(o -> Optional.ofNullable(objectToMap(o).get(treeColumn)).map(pid -> (String.valueOf(pid)).equals(String.valueOf(parentId))).orElse(false)).collect(Collectors.toList());
        if (children != null && children.size() > 0) {
            list.removeAll(children);
            result = new ArrayList();
            for (Object o : children) {
                Map<String, Object> data = objectToMap(o);
                Object id = data.get(column);
                Map record = new HashMap<String, Object>();
                if (!treePlain) {//不显示原始树
                    record.put("id", id);
                    record.put("pid", parentId);
                    record.put("data", data);
                } else
                    record = data;
                record.put("children", dealLeaf(list, column, treeColumn, id, treePlain));
                result.add(record);
            }
        }
        return result;
    }

    /**
     * 导出Xls
     *
     * @param excelTitle   标题
     * @param excelHeaders 表头（数组中的字符串格式：字段名:字段值,...例如："name:姓名"）
     * @param list         数据
     * @param response     HttpServletResponse
     */
    public static void exportXls(String excelTitle, String[] excelHeaders, List list, HttpServletResponse response) {
        try {
            excelTitle = Optional.ofNullable(excelTitle).orElse(String.valueOf(System.currentTimeMillis()));
            HSSFWorkbook workbook = new HSSFWorkbook(); // 创建工作簿对象
            HSSFCellStyle cellStyle = workbook.createCellStyle();//样式
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            HSSFSheet sheet = workbook.createSheet(); // 创建工作表
            HSSFRow rowTitle = sheet.createRow(0);//标题
            HSSFCell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(excelTitle);
            CellRangeAddress cellAddresses = new CellRangeAddress(0, 0, 0, excelHeaders.length - 1);
            sheet.addMergedRegion(cellAddresses);
            cellTitle.setCellStyle(cellStyle);
            RegionUtil.setBorderBottom(BorderStyle.THIN, cellAddresses, sheet);
            RegionUtil.setBorderTop(BorderStyle.THIN, cellAddresses, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, cellAddresses, sheet);
            HSSFRow head = sheet.createRow(1);//表头
            for (int i = 0; i < excelHeaders.length; i++) {
                HSSFCell cell = head.createCell(i);
                cell.setCellType(CellType.STRING);
                String name = excelHeaders[i]; //字段列名信息
                if (name.contains(":"))
                    name = name.split(":")[1];
                HSSFRichTextString text = new HSSFRichTextString(name);
                cell.setCellValue(text);
                cell.setCellStyle(cellStyle);
            }
            int index = 2;
            for (Object o : list) {//内容
                Map map = (Map) o;
                HSSFRow r = sheet.createRow(index++);
                for (int i = 0; i < excelHeaders.length; i++) {
                    String name = excelHeaders[i]; //字段列名信息
                    if (name.contains(":"))
                        name = name.split(":")[0];
                    HSSFCell cell = r.createCell(i);
                    cell.setCellType(CellType.STRING);
                    String value = Optional.ofNullable(map.get(name)).map(String::valueOf).orElse(null);
                    if (isNotNull(value))
                        cell.setCellValue(value);
                    cell.setCellStyle(cellStyle);
                }
            }
            for (int i = 0; i < excelHeaders.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 256 * 6);
            }
            OutputStream out = response.getOutputStream();
            response.setContentType("application/msexcel");
            String filename = new String(excelTitle.getBytes("gbk"), "iso8859-1") + ".xls";
            response.setHeader("Content-disposition", "attachment; filename=" + filename);
            response.setCharacterEncoding("utf-8");
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            logger.error("exportXls error:", e);
        }
    }

    /**
     * 导出Xlsx
     *
     * @param excelTitle   标题
     * @param excelHeaders 表头（数组中的字符串格式：字段名:字段值,...例如："name:姓名"）
     * @param list         数据
     * @param response     HttpServletResponse
     */
    public static void exportXlsx(String excelTitle, String[] excelHeaders, List list, HttpServletResponse response) {
        try {
            excelTitle = Optional.ofNullable(excelTitle).orElse(String.valueOf(System.currentTimeMillis()));
            XSSFWorkbook wb = new XSSFWorkbook();// 声明一个工作簿
            CellStyle cellStyle = wb.createCellStyle();// 全局加线样式
            cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
            cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
            cellStyle.setBorderTop(BorderStyle.THIN);//上边框
            cellStyle.setBorderRight(BorderStyle.THIN);//右边框
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFSheet sheet = wb.createSheet();// 创建sheet页
            XSSFRow rowTitle = sheet.createRow(0);
            Cell cellTitle = rowTitle.createCell(0); // 0列
            cellTitle.setCellValue(excelTitle);
            CellRangeAddress cellAddresses = new CellRangeAddress(0, 0, 0, excelHeaders.length - 1);
            sheet.addMergedRegion(cellAddresses);
            cellTitle.setCellStyle(cellStyle);
            RegionUtil.setBorderBottom(BorderStyle.THIN, cellAddresses, sheet);
            RegionUtil.setBorderTop(BorderStyle.THIN, cellAddresses, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, cellAddresses, sheet);
            XSSFRow head = sheet.createRow(1);// 表头
            for (int i = 0; i < excelHeaders.length; i++) {
                XSSFCell cell = head.createCell(i);
                cell.setCellType(CellType.STRING);
                String headName = excelHeaders[i];
                if (headName.contains(":"))
                    headName = headName.split(":")[1];
                cell.setCellValue(headName);
                cell.setCellStyle(cellStyle);
            }
            int index = 2;
            for (Object o : list) {// 写入内容数据
                Map map = (Map) o;
                XSSFRow r = sheet.createRow(index++);
                for (int i = 0; i < excelHeaders.length; i++) {
                    String name = excelHeaders[i]; // 列名
                    if (name.contains(":"))
                        name = name.split(":")[0];
                    XSSFCell cell = r.createCell(i);
                    cell.setCellType(CellType.STRING);
                    String value = Optional.ofNullable(map.get(name)).map(String::valueOf).orElse(null);
                    if (isNotNull(value))
                        cell.setCellValue(value);
                    cell.setCellStyle(cellStyle);
                }
            }
            for (int i = 0; i < excelHeaders.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 256 * 10);
            }
            OutputStream output = response.getOutputStream();
            response.reset();
            String filename = new String(excelTitle.getBytes("gbk"), "iso8859-1") + ".xlsx";
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            response.setContentType("application/msexcel");
            wb.write(output);
            output.flush();
            wb.close();
        } catch (Exception e) {
            logger.error("exportXlsx error:", e);
        }
    }

    /**
     * 导出Excel
     *
     * @param exportTitle   标题
     * @param exportHeaders 表头（数组中的字符串格式：字段名:字段值,...例如："name:姓名"）
     * @param list          数据
     * @param type          ExcelExportType
     * @param response      HttpServletResponse
     */
    public static void exportFile(String exportTitle, String[] exportHeaders, List list, ExportType type, HttpServletResponse response) {
        if (type != null && ExportType.xlsx.equals(type)) {
            exportXlsx(exportTitle, exportHeaders, list, response);
        } else {
            exportXls(exportTitle, exportHeaders, list, response);
        }
    }

    /**
     * 获取真实IP地址
     *
     * @param request
     * @return
     */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        return ip;
    }

    /**
     * 过滤非法SQL字符串
     *
     * @param input
     * @return
     */
    public static String filterSql(String input) {
        String regex = "execute|exec|insert|delete|update|drop|truncate|grant|use|create";
        return Optional.ofNullable(input).map(string -> string.replaceAll("(?i)" + regex, "")).orElse(input);
    }

    /**
     * 保存文本内容
     *
     * @param path
     * @param text
     * @return
     */
    public static boolean textToFile(String path, String text) {
        File f = new File(path);//向指定文本框内写入
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }


    /**
     * 获取文本内容
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static String readTextFromPath(String path) throws Exception {
        return readTextFromPath(path, null);
    }

    /**
     * 获取文本内容
     *
     * @param path
     * @param enconding
     * @return
     * @throws IOException
     */
    public static String readTextFromPath(String path, String enconding) throws Exception {
        File file = new File(path);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        FileInputStream in = new FileInputStream(file);
        in.read(filecontent);
        in.close();
        return new String(filecontent, Optional.ofNullable(enconding).orElse("UTF-8"));
    }
}
