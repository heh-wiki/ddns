import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeSubDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author heh
 * @date 2021/3/18
 */
public class DDNS {
    private static final String domain = "starshin.net";// 您的域名
    private static final String accessKeyId = "LTAI5tRgmsL5XoQXEyF1YUgu";//您的AccessKey ID
    private static final String secret = "EeDRvsWv5g5SiGVqtjjVCyGZvl8NWG";// 您的AccessKey Secret

    public static void main(String[] args) {
        //创建定时器对象
        Timer t = new Timer();
        //在3秒后执行MyTask类中的run方法,后面每10秒跑一次 delay
        t.schedule(new TimerTaskBean(), 3000, 10000);
    }

    static class TimerTaskBean extends java.util.TimerTask {

        @Override
        public void run() {
            checkDns(domain, accessKeyId, secret);
        }
    }

    /**
     * 获取主域名的所有解析记录列表
     */
    private DescribeSubDomainRecordsResponse describeSubDomainRecords(DescribeSubDomainRecordsRequest request, IAcsClient client) {
        try {
            // 调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            // 发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    /**
     * 获取当前主机公网IP
     */
    private String getCurrenHostIP() {
        // 这里使用jsonip.com第三方接口获取本地IP
        String jsonip = "https://jsonip.com";
        // 接口返回结果
        String result = "";
        BufferedReader in = null;
        try {
            // 使用HttpURLConnection网络请求第三方接口
            URL url = new URL(jsonip);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        //  正则表达式，提取xxx.xxx.xxx.xxx，将IP地址从接口返回结果中提取出来
        String rexp = "(\\d{1,3}\\.){3}\\d{1,3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(result);
        String res = "";
        while (mat.find()) {
            res = mat.group();
            break;
        }
        return res;
    }

    /**
     * 修改解析记录
     */
    private UpdateDomainRecordResponse updateDomainRecord(UpdateDomainRecordRequest request, IAcsClient client) {
        try {
            //  调用SDK发送请求
            return client.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
            //  发生调用错误，抛出运行时异常
            throw new RuntimeException();
        }
    }

    private static void log_print(String functionName, Object result) {
        Gson gson = new Gson();
        System.out.println("-------------------------------" + functionName + "-------------------------------");
        System.out.println(gson.toJson(result));
    }

    /**
     * @param domain      您的域名
     * @param accessKeyId 您的AccessKey ID
     * @param secret      您的AccessKey Secret
     */
    public static void checkDns(String domain, String accessKeyId, String secret) {
        //  设置鉴权参数，初始化客户端
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, secret);
        IAcsClient client = new DefaultAcsClient(profile);
        DDNS ddns = new DDNS();
        //查询指定二级域名的最新解析记录
        DescribeSubDomainRecordsRequest describeSubDomainRecordsRequest = new DescribeSubDomainRecordsRequest();
        describeSubDomainRecordsRequest.setSubDomain(domain);
        DescribeSubDomainRecordsResponse describeSubDomainRecordsResponse = ddns.describeSubDomainRecords(describeSubDomainRecordsRequest, client);
        log_print("describeSubDomainRecords", describeSubDomainRecordsResponse);
        List<DescribeSubDomainRecordsResponse.Record> domainRecords = describeSubDomainRecordsResponse.getDomainRecords();
        //最新的一条解析记录
        if (domainRecords.size() != 0) {
            DescribeSubDomainRecordsResponse.Record record = domainRecords.get(0);
            //  记录ID
            String recordId = record.getRecordId();
            //  记录值
            String recordsValue = record.getValue();
            //  当前主机公网IP
            String currentHostIP = ddns.getCurrenHostIP();
            System.out.println("-------------------------------当前主机公网IP为：" + currentHostIP + "-------------------------------");
            if (!currentHostIP.equals(recordsValue)) {
                //  修改解析记录
                UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                //  主机记录@
                updateDomainRecordRequest.setRR("@");
                //  记录ID
                updateDomainRecordRequest.setRecordId(recordId);
                //  将主机记录值改为当前主机IP
                updateDomainRecordRequest.setValue(currentHostIP);
                //  解析记录类型
                updateDomainRecordRequest.setType("A");
                UpdateDomainRecordResponse updateDomainRecordResponse = ddns.updateDomainRecord(updateDomainRecordRequest, client);
                log_print("updateDomainRecord", updateDomainRecordResponse);
            }
        }
    }


}
