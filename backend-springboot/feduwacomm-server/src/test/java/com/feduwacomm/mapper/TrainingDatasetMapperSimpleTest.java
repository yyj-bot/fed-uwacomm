package com.feduwacomm.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Commit;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import com.feduwacomm.entity.TrainingData;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TrainingDatasetMapperSimpleTest {

    @Autowired
    private TrainingDatasetMapper trainingDatasetMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Test
    public void testUpsertAndSelect() {
        String testId = "test-" + System.currentTimeMillis() % 10000;
        String testName = "简单测试数据集";
        String description = "这是一个简单的测试";
        String dataType = "ACOUSTIC";
        String status = "READY";
        String metadata = "{\"test\": true}";

        // 插入数据
        int result = trainingDatasetMapper.upsertDataset(testId, testName, description, dataType, status, metadata);
        System.out.println("插入结果: " + result);

        // 手动刷新SqlSession
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.commit();
            System.out.println("手动提交会话");
        } catch (Exception e) {
            System.out.println("手动提交异常: " + e.getMessage());
        }

        // 使用新的selectByIdAsMap方法
        System.out.println("尝试查询ID: " + testId);
        Map<String, Object> dataset = trainingDatasetMapper.selectByIdAsMap(testId);
        System.out.println("selectByIdAsMap查询结果: " + dataset);

        // 直接JDBC查询验证
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, name, description, data_type, status, metadata, upload_time FROM training_dataset WHERE id = ?")) {
            stmt.setString(1, testId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("JDBC查询结果: id=" + rs.getString("id") + ", name=" + rs.getString("name"));
            } else {
                System.out.println("JDBC查询结果: 未找到记录");
            }
        } catch (Exception e) {
            System.out.println("JDBC查询异常: " + e.getMessage());
        }

        assertNotNull(dataset, "selectByIdAsMap查询结果不应为null，testId: " + testId);
        assertEquals(testId, dataset.get("id"));
        assertEquals(testName, dataset.get("name"));
        assertEquals(description, dataset.get("description"));
    }
}