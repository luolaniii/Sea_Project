package com.boot.study.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek AI 辅助决策服务
 * <p>
 * 调用 DeepSeek 兼容 OpenAI 协议的 chat completions 接口，将站点的当前海况
 * 数据 + 历史趋势作为上下文，由模型给出未来 24 小时的海况预测和作业建议。
 * <p>
 * 使用前需在环境变量中设置：DEEPSEEK_API_KEY=xxxx
 */
@Slf4j
@Service
public class AiForecastService {

    @Value("${ai.deepseek.enabled:false}")
    private boolean enabled;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.timeout-ms:60000}")
    private int timeoutMs;

    /**
     * 基于站点当前海况上下文，请求 DeepSeek 给出未来海况预测和建议。
     *
     * @param stationContext key-value 形式的当前快照（含站点名/坐标/温度/波高/风速等）
     * @return AI 文本响应（Markdown 格式）
     */
    public Map<String, Object> forecast(Map<String, Object> stationContext) {
        Map<String, Object> result = new HashMap<>();
        result.put("provider", "deepseek");
        result.put("model", model);

        if (!enabled) {
            result.put("error", "AI 辅助决策未启用（ai.deepseek.enabled=false）");
            return result;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            result.put("error", "未配置 DEEPSEEK_API_KEY 环境变量");
            return result;
        }

        try {
            String prompt = buildPrompt(stationContext);

            JSONArray messages = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put(
                    "content",
                    "你是一名资深海洋气象预报员和海洋作业安全顾问。" +
                            "用户会给你某个 NDBC 浮标站点的当前海况快照，请基于以下要求给出回复：\n" +
                            "1. 用 Markdown 输出。\n" +
                            "2. 先用 1 段总评（≤80 字）描述当前海况。\n" +
                            "3. 接着分别给出未来 6h / 12h / 24h 的预测（波高、风速、温度的方向性变化）。\n" +
                            "4. 给出作业建议（小型船只/钓鱼/冲浪/工程作业各 1 条）。\n" +
                            "5. 最后用 ⚠️ 标记需要警惕的风险点。\n" +
                            "重要：你只能基于给定的数据和经验做合理外推，不要编造具体气象事件。");
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", prompt);
            messages.add(sys);
            messages.add(user);

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", 0.5);
            body.put("max_tokens", 1200);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Accept", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(body.toJSONString(), headers);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) Duration.ofMillis(timeoutMs).toMillis());
            factory.setReadTimeout((int) Duration.ofMillis(timeoutMs).toMillis());
            RestTemplate rt = new RestTemplate(factory);

            String url = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "v1/chat/completions";
            long t0 = System.currentTimeMillis();
            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.POST, entity, String.class);
            long elapsed = System.currentTimeMillis() - t0;

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JSONObject json = JSONObject.parseObject(resp.getBody());
                JSONArray choices = json.getJSONArray("choices");
                String content = "";
                if (choices != null && !choices.isEmpty()) {
                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                    if (msg != null) content = msg.getString("content");
                }
                JSONObject usage = json.getJSONObject("usage");
                result.put("content", content);
                result.put("elapsedMs", elapsed);
                if (usage != null) {
                    result.put("promptTokens", usage.getInteger("prompt_tokens"));
                    result.put("completionTokens", usage.getInteger("completion_tokens"));
                    result.put("totalTokens", usage.getInteger("total_tokens"));
                }
                return result;
            }
            result.put("error", "DeepSeek 返回非2xx: " + resp.getStatusCodeValue());
            return result;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 4xx / 5xx，把响应体回传，便于前端看到 invalid_api_key / insufficient_balance 等
            String body = e.getResponseBodyAsString();
            log.error("DeepSeek HTTP 异常 status={} body={}", e.getStatusCode(), body, e);
            String hint;
            int code = e.getStatusCode().value();
            if (code == 401) hint = "API Key 无效或已过期，请检查 DEEPSEEK_API_KEY 环境变量";
            else if (code == 402) hint = "DeepSeek 账户余额不足，请前往 platform.deepseek.com 充值";
            else if (code == 429) hint = "DeepSeek 请求过于频繁，已被限流";
            else if (code >= 500) hint = "DeepSeek 服务端错误，请稍后重试";
            else hint = "DeepSeek 请求被拒绝";
            result.put("error", hint + "（HTTP " + code + "）：" + truncate(body, 300));
            result.put("httpStatus", code);
            return result;
        } catch (ResourceAccessException e) {
            // 网络/SSL/超时
            log.error("DeepSeek 网络异常", e);
            String msg = String.valueOf(e.getMessage());
            String hint;
            if (msg.contains("timed out") || msg.contains("timeout")) hint = "调用超时（>" + timeoutMs + "ms），可能是网络慢或被防火墙拦截";
            else if (msg.contains("UnknownHost")) hint = "无法解析 api.deepseek.com，检查网络/DNS";
            else if (msg.contains("SSL") || msg.contains("handshake")) hint = "SSL 握手失败，请检查 JDK 证书或代理";
            else if (msg.contains("Connection refused")) hint = "无法连接 DeepSeek 服务";
            else hint = "网络异常";
            result.put("error", hint + "：" + msg);
            return result;
        } catch (Exception e) {
            log.error("调用 DeepSeek 失败", e);
            result.put("error", "调用失败：" + e.getClass().getSimpleName() + " - " + e.getMessage());
            return result;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String buildPrompt(Map<String, Object> ctx) {
        if (ctx == null) ctx = Collections.emptyMap();
        StringBuilder sb = new StringBuilder();
        sb.append("# 海况快照\n\n");
        appendKv(sb, ctx, "stationName", "站点名称");
        appendKv(sb, ctx, "stationId", "站点编号");
        appendKv(sb, ctx, "longitude", "经度");
        appendKv(sb, ctx, "latitude", "纬度");
        appendKv(sb, ctx, "currentTime", "观测时间");
        sb.append("\n## 实测参数\n");
        appendKv(sb, ctx, "temperature", "水温(°C)");
        appendKv(sb, ctx, "salinity", "盐度(PSU)");
        appendKv(sb, ctx, "seaLevel", "潮位(m)");
        appendKv(sb, ctx, "waveHeight", "有效波高Hs(m)");
        appendKv(sb, ctx, "wavePeriod", "平均周期APD(s)");
        appendKv(sb, ctx, "waveDirection", "主波向(°)");
        appendKv(sb, ctx, "windSpeed", "风速(m/s)");
        appendKv(sb, ctx, "atmPressure", "气压(hPa)");
        sb.append("\n## 已计算指标\n");
        appendKv(sb, ctx, "stabilityLevel", "稳定性等级");
        appendKv(sb, ctx, "comfortScore", "海洋作业舒适度评分(0-100)");
        appendKv(sb, ctx, "abnormalLevel1", "一级异常告警");
        appendKv(sb, ctx, "abnormalLevel2", "二级异常告警");
        if (ctx.get("history") != null) {
            sb.append("\n## 近 24h 趋势摘要\n");
            sb.append(String.valueOf(ctx.get("history"))).append("\n");
        }
        sb.append("\n请基于以上数据给出预测和建议。");
        return sb.toString();
    }

    private static void appendKv(StringBuilder sb, Map<String, Object> ctx, String key, String label) {
        Object v = ctx.get(key);
        if (v == null) return;
        sb.append("- ").append(label).append("：").append(v).append("\n");
    }
}
