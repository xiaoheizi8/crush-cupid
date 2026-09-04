package cn.yzfy.crushcupidserver.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.util.SaTokenConsts;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.agent.StickerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @className WebMvcConfig
 * @description Web MVC 配置：表情包/上传图片静态资源映射 + Sa-Token 登录态拦截。
 * <p>
 * 鉴权规则：
 * <ul>
 *   <li>白名单：/api/auth/**（注册/登录/验证码）、/api/uploads/**、/api/stickers/**、/api/health</li>
 *   <li>其余 /api/** 全部要求登录（登录校验在 SaInterceptor 内完成）</li>
 *   <li>SSE 长连接（chat/build/push）浏览器 EventSource 无法自定义 header，
 *       允许从 url 查询参数读 token：前端传 <code>?satoken=xxx</code> 或 <code>?token=xxx</code></li>
 * </ul>
 * @author 一朝风月
 * @code config
 * @createTime 2026-08-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 表情包：classpath:stickers/ 下的静态资源（前端 img 直连）
        registry.addResourceHandler(StickerService.URL_PREFIX + "**")
                .addResourceLocations("classpath:/stickers/");
        // 对话图片：磁盘上传目录（/api/uploads/** -> file:./uploads/），历史回显用。
        // pattern 必须是 "前缀/**"（带斜杠）：拼成 "前缀**" 会因 AntPathMatcher 按段匹配
        // 无法命中多级子路径（如 /api/uploads/20260828/x.jpg），导致所有上传图片 404
        String prefix = uploadProperties.getUrlPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = UploadProperties.DEFAULT_URL_PREFIX;
        }
        String handler = prefix.endsWith("/") ? prefix + "**" : prefix + "/**";
        String location = "file:" + FileUtil.getAbsolutePath(uploadProperties.getDir()).replace('\\', '/');
        if (!location.endsWith("/")) {
            location += "/";
        }
        log.info("注册静态资源映射：handler={} -> location={} (dir={}, urlPrefix={})",
                handler, location, uploadProperties.getDir(), prefix);
        registry.addResourceHandler(handler).addResourceLocations(location);
    }

    /** SSE 端点：浏览器 EventSource 无法自定义请求头，允许通过 url 查询参数携带 token */
    private static final String[] SSE_PATHS = {
            "/api/chat/**", "/api/build/**", "/api/push/**"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            var request = SaHolder.getRequest();
            // CORS 预检请求（OPTIONS）不携带 token，直接放行
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return;
            }
            // SSE 长连接：浏览器 EventSource 无法自定义请求头，
            // 当请求头无 token 时，允许从 url 查询参数读取 token 注入 Sa-Token 存储供 StpUtil 读取
            String headerToken = request.getHeader(SaManager.getConfig().getTokenName());
            if (StrUtil.isBlank(headerToken) && isSsePath(request.getRequestPath())) {
                String queryToken = request.getParam("satoken");
                if (StrUtil.isBlank(queryToken)) {
                    queryToken = request.getParam("token");
                }
                if (StrUtil.isNotBlank(queryToken)) {
                    SaHolder.getStorage().set(SaTokenConsts.JUST_CREATED_SAVE_KEY, queryToken);
                    log.debug("[Sa-Token] SSE 请求通过 query 参数注入 token: {}", queryToken);
                }
            }
            // 静态资源与认证接口放行
            SaRouter.match("/api/auth/**", "/api/uploads/**", "/api/stickers/**", "/api/health")
                    .check(r -> {
                    })
                    // 其余 /api/** 全部要求登录
                    .notMatch("/api/**")
                    .check(r -> StpUtil.checkLogin());
        }))
                // 只拦截 /api/**，静态资源/错误路径不拦
                .addPathPatterns("/api/**");
    }

    private boolean isSsePath(String path) {
        if (StrUtil.isBlank(path)) {
            return false;
        }
        for (String ssePath : SSE_PATHS) {
            if (path.startsWith(ssePath)) {
                return true;
            }
        }
        return false;
    }
}