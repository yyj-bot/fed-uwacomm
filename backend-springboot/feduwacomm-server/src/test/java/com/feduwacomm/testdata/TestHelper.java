package com.feduwacomm.testdata;

import com.feduwacomm.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.mockito.ArgumentMatcher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * 测试助手类
 * 提供Mock设置、参数匹配器和常用测试逻辑
 */
public class TestHelper {

    /**
     * JWT Claims 构建器
     */
    public static class JwtClaims {
        
        public static Claims validAccessTokenClaims(String userId, String username, String role) {
            Claims claims = new DefaultClaims();
            claims.put("userId", userId);
            claims.put("username", username);
            claims.put("role", role);
            claims.put("type", "access");
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", (System.currentTimeMillis() / 1000) + 86400); // 24 hours
            return claims;
        }
        
        public static Claims validRefreshTokenClaims(String userId) {
            Claims claims = new DefaultClaims();
            claims.put("userId", userId);
            claims.put("type", "refresh");
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", (System.currentTimeMillis() / 1000) + 604800); // 7 days
            return claims;
        }
        
        public static Claims expiredTokenClaims(String userId) {
            Claims claims = new DefaultClaims();
            claims.put("userId", userId);
            claims.put("type", "access");
            claims.put("iat", (System.currentTimeMillis() / 1000) - 86400);
            claims.put("exp", (System.currentTimeMillis() / 1000) - 3600); // Expired 1 hour ago
            return claims;
        }
    }

    /**
     * 参数匹配器
     */
    public static class Matchers {
        
        /**
         * 匹配用户实体的特定字段
         */
        public static User userWithUsername(String username) {
            return argThat(user -> user != null && username.equals(user.getUsername()));
        }
        
        public static User userWithEmail(String email) {
            return argThat(user -> user != null && email.equals(user.getEmail()));
        }
        
        public static User userWithRole(String role) {
            return argThat(user -> user != null && role.equals(user.getRole()));
        }
        
        public static User userWithStatus(String status) {
            return argThat(user -> user != null && status.equals(user.getStatus()));
        }
        
        /**
         * 组合匹配器
         */
        public static User userMatching(ArgumentMatcher<User>... matchers) {
            return argThat(user -> {
                if (user == null) return false;
                for (ArgumentMatcher<User> matcher : matchers) {
                    if (!matcher.matches(user)) return false;
                }
                return true;
            });
        }
        
        /**
         * 时间范围匹配器
         */
        public static LocalDateTime timeWithinMinutes(LocalDateTime expected, int minutes) {
            return argThat(time -> {
                if (time == null) return false;
                LocalDateTime start = expected.minusMinutes(minutes);
                LocalDateTime end = expected.plusMinutes(minutes);
                return !time.isBefore(start) && !time.isAfter(end);
            });
        }
        
        /**
         * 密码hash匹配器（验证密码是否被正确加密）
         */
        public static String hashedPassword(String plainPassword) {
            return argThat(hashedPwd -> hashedPwd != null 
                    && !hashedPwd.equals(plainPassword) 
                    && hashedPwd.length() > 20);
        }
    }

    /**
     * Mock状态管理器
     * 用于模拟数据库状态变化，提供状态一致性
     */
    public static class MockDatabase {
        
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final Map<String, Object> entities = new ConcurrentHashMap<>();
        
        public MockDatabase() {
            // 初始化一些基础测试数据
            initializeTestData();
        }
        
        private void initializeTestData() {
            // 添加默认测试用户
            User testUser = TestDataBuilder.Users.validUser().build();
            users.put(testUser.getId(), testUser);
        }
        
        public User insertUser(User user) {
            users.put(user.getId(), user);
            return user;
        }
        
        public User selectUserById(String userId) {
            return users.get(userId);
        }
        
        public User selectUserByUsername(String username) {
            return users.values().stream()
                    .filter(user -> username.equals(user.getUsername()))
                    .findFirst()
                    .orElse(null);
        }
        
        public User selectUserByEmail(String email) {
            return users.values().stream()
                    .filter(user -> email.equals(user.getEmail()))
                    .findFirst()
                    .orElse(null);
        }
        
        public int updateUser(User user) {
            if (users.containsKey(user.getId())) {
                users.put(user.getId(), user);
                return 1;
            }
            return 0;
        }
        
        public int deleteUser(String userId) {
            return users.remove(userId) != null ? 1 : 0;
        }
        
        public int countUsers() {
            return users.size();
        }
        
        public void clear() {
            users.clear();
            entities.clear();
            initializeTestData();
        }
        
        /**
         * 通用实体操作
         */
        @SuppressWarnings("unchecked")
        public <T> T getEntity(String key, Class<T> type) {
            Object entity = entities.get(key);
            return type.isInstance(entity) ? (T) entity : null;
        }
        
        public void putEntity(String key, Object entity) {
            entities.put(key, entity);
        }
        
        public Object removeEntity(String key) {
            return entities.remove(key);
        }
    }

    /**
     * 时间相关的测试助手
     */
    public static class TimeHelper {
        
        /**
         * 验证时间戳是否在指定范围内
         */
        public static boolean isWithinRange(LocalDateTime time, LocalDateTime expected, int toleranceMinutes) {
            if (time == null || expected == null) return false;
            LocalDateTime start = expected.minusMinutes(toleranceMinutes);
            LocalDateTime end = expected.plusMinutes(toleranceMinutes);
            return !time.isBefore(start) && !time.isAfter(end);
        }
        
        /**
         * 创建时间戳，用于测试时序
         */
        public static LocalDateTime[] createTimeSequence(int count, int intervalMinutes) {
            LocalDateTime[] times = new LocalDateTime[count];
            LocalDateTime base = LocalDateTime.now();
            for (int i = 0; i < count; i++) {
                times[i] = base.plusMinutes(i * intervalMinutes);
            }
            return times;
        }
    }

    /**
     * 断言助手
     */
    public static class Assertions {
        
        /**
         * 验证用户对象的基本字段
         */
        public static void assertUserValid(User user) {
            if (user == null) {
                throw new AssertionError("User should not be null");
            }
            if (user.getId() == null || user.getId().trim().isEmpty()) {
                throw new AssertionError("User ID should not be null or empty");
            }
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                throw new AssertionError("Username should not be null or empty");
            }
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                throw new AssertionError("Email should not be null or empty");
            }
        }
        
        /**
         * 验证密码是否被正确加密
         */
        public static void assertPasswordEncrypted(String plainPassword, String hashedPassword) {
            if (hashedPassword == null || hashedPassword.equals(plainPassword)) {
                throw new AssertionError("Password should be encrypted");
            }
            if (hashedPassword.length() < 20) {
                throw new AssertionError("Encrypted password seems too short");
            }
        }
        
        /**
         * 验证时间戳是否合理（不能是未来时间，不能太久以前）
         */
        public static void assertReasonableTimestamp(LocalDateTime timestamp) {
            if (timestamp == null) {
                throw new AssertionError("Timestamp should not be null");
            }
            LocalDateTime now = LocalDateTime.now();
            if (timestamp.isAfter(now.plusMinutes(5))) {
                throw new AssertionError("Timestamp should not be in the future");
            }
            if (timestamp.isBefore(now.minusYears(1))) {
                throw new AssertionError("Timestamp seems too old");
            }
        }
    }

    /**
     * 常用的测试常量
     */
    public static class Constants {
        public static final String VALID_PASSWORD = "password123";
        public static final String INVALID_PASSWORD = "wrongpassword";
        public static final String VALID_EMAIL = "test@example.com";
        public static final String INVALID_EMAIL = "invalid-email";
        public static final String VALID_USERNAME = "testuser";
        public static final String ADMIN_USERNAME = "admin";
        public static final String TEST_IP = "192.168.1.100";
        public static final String TEST_TOKEN_PREFIX = "Bearer ";
        
        // 角色常量
        public static final String ROLE_ADMIN = "ADMIN";
        public static final String ROLE_RESEARCHER = "RESEARCHER";
        public static final String ROLE_VIEWER = "VIEWER";
        
        // 状态常量
        public static final String STATUS_ACTIVE = "ACTIVE";
        public static final String STATUS_LOCKED = "LOCKED";
        public static final String STATUS_SUSPENDED = "SUSPENDED";
    }
}