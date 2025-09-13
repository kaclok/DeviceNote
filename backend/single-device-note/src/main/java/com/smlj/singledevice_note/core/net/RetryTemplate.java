package com.smlj.singledevice_note.core.net;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class RetryTemplate {
    private org.springframework.retry.support.RetryTemplate retryTemplate;

    private static final int max_retries = 10;
    private static final long retry_delay = 10000;

    public org.springframework.retry.support.RetryTemplate getRetryTemplate() {
        if (retryTemplate == null) {
            retryTemplate = new org.springframework.retry.support.RetryTemplate();
            retryTemplate.setRetryPolicy(new MaxAttemptsRetryPolicy(max_retries));

            FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
            backOffPolicy.setBackOffPeriod(retry_delay); // 每次重试等待10秒
            retryTemplate.setBackOffPolicy(backOffPolicy);
        }
        return retryTemplate;
    }

    public void dispose() {
        if(retryTemplate != null) {

        }
    }
}
