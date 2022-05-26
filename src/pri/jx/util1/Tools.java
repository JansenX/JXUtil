package pri.jx.util1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Tools {
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 将字符串的编码格式转换为utf-8
	 * 
	 * @param str
	 * @return Name = new
	 * String(Name.getBytes("ISO-8859-1"), "utf-8");
	 */
	public static String toUTF8(String str) throws Exception{
		if (isEmpty(str)) {
			return "";
		}
		if (str.equals(new String(str.getBytes("GB2312"), "GB2312"))) {
			str = new String(str.getBytes("GB2312"), "utf-8");
			return str;
		}
		if (str.equals(new String(str.getBytes("ISO-8859-1"), "ISO-8859-1"))) {
			str = new String(str.getBytes("ISO-8859-1"), "utf-8");
			return str;
		}
		if (str.equals(new String(str.getBytes("GBK"), "GBK"))) {
			str = new String(str.getBytes("GBK"), "utf-8");
			return str;
		}
		return str;
	}
	

	/**
	 * 判断字符串是否为空
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		// 如果字符串不为null，去除空格后值不与空字符串相等的话，证明字符串有实质性的内容
		if (!StringUtils.isBlank(str)) {
			return false;// 不为空
		}
		return true;// 为空
	}

	/**
	 *ResultSet转换为JSON数组
	 *@param ResultSet
	 *@return JSONArray(返回以自定义列名为key的JSON数组)
	 */
    public static JSONArray resultSetToJsonArry(ResultSet rs) throws Exception {
        JSONArray array = new JSONArray();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            JSONObject jsonObj = new JSONObject();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                if("NUMBER".equals(metaData.getColumnTypeName(i))){//getColumnType 2
                	double value = rs.getDouble(columnName);
                	jsonObj.put(columnName, value);
                }  
                else if("DATE".equals(metaData.getColumnTypeName(i))){//getColumnType 93
            		String value = rs.getTimestamp(columnName)==null?"":sdf.format(rs.getTimestamp(columnName));
            		jsonObj.put(columnName, value);
                }else{
                	String value = rs.getString(columnName);
                	jsonObj.put(columnName, value);
                }
            }
            array.add(jsonObj);
        }
        return array;
    }
    
    /**
     * 返回当前调用方法的类名、方法名以及行数*/
    public static String getDebugInfo(){
		StackTraceElement[] lvStacks = Thread.currentThread().getStackTrace();
		return  sdf.format(System.currentTimeMillis()) + " ClassName:"+lvStacks[2].getClassName() + ",FunctionName:" + lvStacks[2].getMethodName()
			    +",Line:"+ lvStacks[2].getLineNumber() + "\n";
	}

    /**
     * 发送https请求 
     * @param requestUrl  请求地址
     * @param requestMethod  请求方式（GET、POST）
     * @param outputStr  提交的数据
     * @return JSONObject(通过JSONObject.get(key)的方式获取json对象的属性值)
     * @throws Exception 
     */
    public static JSONObject httpsRequest(String requestUrl, String requestMethod, 
    		String outputStr) throws Exception {
        JSONObject json = new JSONObject();
        // 创建SSLContext对象，并使用我们指定的信任管理器初始化
        TrustManager[] tm = null;
        SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
        sslContext.init(null, tm, new java.security.SecureRandom());
        // 从上述SSLContext对象中得到SSLSocketFactory对象
        SSLSocketFactory ssf = sslContext.getSocketFactory();

        URL url = new URL(requestUrl);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(ssf);

        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        // 设置请求方式（GET/POST）
        conn.setRequestMethod(requestMethod.toUpperCase());

        // 当outputStr不为null时向输出流写数据
        if (null != outputStr) {
            OutputStream outputStream = conn.getOutputStream();
            // 注意编码格式
            outputStream.write(outputStr.getBytes("UTF-8"));
            outputStream.close();
        }

        // 从输入流读取返回内容
        InputStream inputStream = conn.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String str = null;
        StringBuffer buffer = new StringBuffer();
        while ((str = bufferedReader.readLine()) != null) {
            buffer.append(str);
        }
        // 释放资源
        bufferedReader.close();
        inputStreamReader.close();
        inputStream.close();
        inputStream = null;
        conn.disconnect();
        json = JSONObject.parseObject(buffer.toString());
        String errcode = json.getString("errcode");
        if (!StringUtils.isBlank(errcode) && !errcode.equals("0")) {
            throw new Exception("ERRCODE:"+ errcode);
        }
        return json;
    }
    
    /**
     * 发送Http Get请求*/
    public static String httpURLGETCase(String methodUrl) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String line = null;
        StringBuilder result = new StringBuilder();
        URL url = new URL(methodUrl);
        connection = (HttpURLConnection) url.openConnection();// 根据URL生成HttpURLConnection
        connection.setRequestMethod("GET");// 默认GET请求
        connection.connect();// 建立TCP连接
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));// 发送http请求
            // 循环读取流
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.getProperty("line.separator"));// "\n"
            }
        }
        reader.close();
        connection.disconnect();
        return result==null?"":result.toString();
    }
    
    /**
     * 发送Http Post请求*/
    public static String httpURLPOSTCase(String methodUrl,String data) {
        HttpURLConnection connection = null;
        PrintWriter pw = null;
        BufferedReader reader = null;
        String line = "";
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(methodUrl);
            connection = (HttpURLConnection)url.openConnection();// 根据URL生成HttpURLConnection
            connection.setDoOutput(true);// 设置是否向connection输出，因为这个是post请求，参数要放在http正文内，因此需要设为true,默认情况下是false
            connection.setDoInput(true); // 设置是否从connection读入，默认情况下是true;
            connection.setRequestMethod("POST");// 设置请求方式为post,默认GET请求
            connection.setUseCaches(false);// post请求不能使用缓存设为false
            connection.setConnectTimeout(10000);// 连接主机的超时时间
            connection.setReadTimeout(10000);// 从主机读取数据的超时时间
            connection.setInstanceFollowRedirects(true);// 设置该HttpURLConnection实例是否自动执行重定向
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");// 连接复用
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.connect();// 建立TCP连接,getOutputStream会隐含的进行connect,所以此处可以不要

            pw = new PrintWriter(connection.getOutputStream());// 创建输入输出流,用于往连接里面输出携带的参数
            pw.write("data="+data);//json数据
            pw.flush();
            pw.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));// 发送http请求
                // 循环读取流
                while (null != (line = reader.readLine())) {
                    result.append(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return result.toString();
    }
    
    /** 
     * 使用NumberFormat,保留小数点后两位 
     * value 被修约参数
     * count 保留几位小数
     */  
    public static double numberFormat(double value,int count) {  
  
        NumberFormat nf = NumberFormat.getNumberInstance();  
        nf.setMaximumFractionDigits(2);  
        /* 
         * setMinimumFractionDigits设置成2 
         *  
         * 如果不这么做，那么当value的值是100.00的时候返回100 
         *  
         * 而不是100.00 
         */  
        nf.setMinimumFractionDigits(count);  
        nf.setRoundingMode(RoundingMode.HALF_UP);  
        /* 
         * 如果想输出的格式用逗号隔开，可以设置成true 
         */  
        nf.setGroupingUsed(false);  
        return Double.parseDouble(nf.format(value));  
    }
    
    /**数字类型字符串转换成数字再相加
     * value：数字类型字符串
     * incremental：加数
     * */
    public static String calculateIntegerForStringValue(String value, int incremental){
		if (value == null || value.length() == 0)
			throw new IllegalArgumentException("String Value is invalid :" + value);
		
		int length = value.length();
		String digit = "";
		for (int i=0; i<length; i++)
			digit += "0";
		
		DecimalFormat decimalFormat = new DecimalFormat(digit);
		Long number = null;
		try{
			number = (Long)decimalFormat.parse(value);			
		}catch (ParseException pe){
			return value;
		} 
		// long value
		int result = number.intValue() + incremental;
		return decimalFormat.format((long)result);
	}
    
    /**调用webservice接口
     * url:调用路径
     * TargetNameSpace:命名空间名，可在浏览器输入接口地址查看
     * serviceName:调用方法名
     * keys Object[] 对象数组类型:方法参数名
     * values Object[] 对象数组类型:参数值
     * */
    public static String webServiceAxisCase(String url,String TargetNameSpace,String serviceName,
    		Object[] keys, Object[] values) throws Exception{
    	if(keys.length != values.length)
			throw new IllegalArgumentException("接口方法参数与参数值不匹配！");
    	String result = "";
    	try {  
            String endpoint = url;  
            // 直接引用远程的wsdl文件
            Service service = new Service();  
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new URL(endpoint));  
            //call.setOperationName(serviceName);//调用的方法名 当这种调用不到的时候,可以使用下面的,加入命名空间名
            call.setOperationName(new QName(TargetNameSpace, serviceName));// 调用的方法名 加入命名空间名
            for(int i = 0; i < values.length; i++){
    			//String key = (String) keys[i]; //方法参数
    			Object value = values[i]; //参数值
    			if(value == null){
    				call.addParameter("arg"+i, XMLType.XSD_STRING, ParameterMode.IN);
    			} else if(value instanceof String){
    				call.addParameter("arg"+i, XMLType.XSD_STRING, ParameterMode.IN);
    			} else if(value instanceof Integer){
    				call.addParameter("arg"+i, XMLType.XSD_INTEGER, ParameterMode.IN);
    			} else if(value instanceof Boolean){
    				call.addParameter("arg"+i, XMLType.XSD_BOOLEAN, ParameterMode.IN);
    			}
    			
    		}
            call.setReturnType(XMLType.XSD_STRING);// 设置返回类型  
            if(values.length == 0)
            	result = (String) call.invoke(new Object[]{});  
            else
            	result = (String) call.invoke(values);  
            // 给方法传递参数，并且调用方法  
        } catch (Exception e) {  
            System.err.println(e.toString());  
        }  
    	return result;
    }
}
