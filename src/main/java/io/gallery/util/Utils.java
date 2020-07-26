package io.gallery.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
                if (o == null) {
                    map.remove(s);
                }
            }
        } else {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
                PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor property : propertyDescriptors) {
                    String key = property.getName();
                    if (key.compareToIgnoreCase("class") == 0) {
                        continue;
                    }
                    Method getter = property.getReadMethod();
                    Object value = getter != null ? getter.invoke(obj) : null;
                    if (value != null) {
                        map.put(key, value);
                    }
                }
            } catch (Exception e) {
                logger.error("convert error:" + e.getMessage());
            }
        }
        return map;
    }

    public static <T> T mapToBean(Map<String, Object> map, Class<T> clazz) {
        T bean = null;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            bean = clazz.newInstance();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (map.containsKey(key)) {
                    setParamter(map, bean, key, property);
                }
            }
        } catch (Exception e) {
            logger.error("mapToBean error:" + e.getMessage());
        }
        return bean;
    }

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

    public static List<String> classKeyToMap(Class<?> clazz) {
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

    public static Map merge(Object source, Map target) {
        if (target != null) {
            target.putAll(objectToMap(source));
        }
        return target;
    }

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
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(input.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(16);
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
}
