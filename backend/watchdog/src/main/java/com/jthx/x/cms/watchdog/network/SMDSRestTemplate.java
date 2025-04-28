package com.jthx.x.cms.watchdog.network;

import com.jthx.x.cms.watchdog.pojo.request.IndicatorRequestInfo;
import com.jthx.x.cms.watchdog.pojo.request.TokenRequestInfo;
import com.jthx.x.cms.watchdog.pojo.response.IndicatorResponseInfo;
import com.jthx.x.cms.watchdog.pojo.response.R;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Service
public class SMDSRestTemplate {
    private final RestTemplate restTemplate = new RestTemplate();
    private RetryTemplate retryTemplate;

    private final String requestIndicatorUrl = "http://10.8.54.110:8721/macs/v2/realtime/read/findPoints";
    private String token;
    private final String requestTokenUrl = "http://10.8.54.110:8721/macs/v1/account/login";

    private static final int max_retries = 10;
    private static final long retry_delay = 10000;

    public RetryTemplate getRetryTemplate() {
        if (retryTemplate == null) {
            retryTemplate = new RetryTemplate();
            retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(max_retries));

            FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
            backOffPolicy.setBackOffPeriod(retry_delay); // 每次重试等待10秒
            retryTemplate.setBackOffPolicy(backOffPolicy);
        }
        return retryTemplate;
    }

    /**
     * 通过网络接口批量获取实时数据
     *
     * @param requestInfo
     * @return
     */
    public IndicatorResponseInfo getIndicatorInfo(IndicatorRequestInfo requestInfo) {
        HttpHeaders headers = new HttpHeaders();
        String token = getToken();
        if (token == null || token.isEmpty()) {
            // 需要在这里打印实时日志，用于告知维护人员，接口获取错误，保证服务的可用性
            return null;
        }
        headers.set("token", token);
        HttpEntity<IndicatorRequestInfo> entity = new HttpEntity<>(requestInfo, headers);
        ResponseEntity<IndicatorResponseInfo> response = null;
        try {
            response = getRetryTemplate().execute(context -> {
                return restTemplate.exchange(requestIndicatorUrl, HttpMethod.POST, entity, IndicatorResponseInfo.class);
            });
        } catch (Exception e) {
            System.out.println(new Date());
            throw new RuntimeException(e);
        }

        return response == null ? null : response.getBody();
    }

    /**
     * 通过网络接口获取token
     *
     * @return
     */
    public String getToken() {
        if (token != null) {
            return token;
        }
        R responseInfo = null;
        try {
            responseInfo = getRetryTemplate().execute(context -> {
                return restTemplate.postForObject(requestTokenUrl, getTokenRequestInfo(), R.class);
            });
        } catch (Exception e) {
            System.out.println(new Date());
            throw e;
        }

        token = (responseInfo == null || responseInfo.getData() == null) ? null : responseInfo.getData().get("token");
        return token;
    }

    /**
     * 获取token的请求体
     *
     * @return
     */
    private TokenRequestInfo getTokenRequestInfo() {
        return new TokenRequestInfo("mes", "123456");
    }
}
