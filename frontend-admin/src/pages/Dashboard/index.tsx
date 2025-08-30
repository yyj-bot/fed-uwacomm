import React from 'react'
import { Row, Col, Card, Statistic, Typography } from 'antd'
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  DatabaseOutlined,
  ShareAltOutlined,
  BarChartOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'

const { Title, Paragraph } = Typography

const Dashboard: React.FC = () => {
  return (
    <div>
      <Title level={2}>系统总览</Title>
      <Paragraph type="secondary">
        水声联邦学习系统运行状态和关键指标概览
      </Paragraph>

      <Row gutter={[16, 16]} style={{ marginTop: '24px' }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="环境样本数"
              value={301}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#3f8600' }}
              suffix={<ArrowUpOutlined />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="活跃客户端"
              value={3}
              prefix={<ShareAltOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="模型准确率"
              value={85.6}
              precision={1}
              suffix="%"
              prefix={<BarChartOutlined />}
              valueStyle={{ color: '#cf1322' }}
              suffix={<ArrowDownOutlined />}
            />
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="系统状态"
              value="正常运行"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: '24px' }}>
        <Col span={24}>
          <Card title="功能模块导航" size="small">
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <Card size="small" hoverable>
                  <Title level={4}>环境分析</Title>
                  <Paragraph>
                    BELLHOP仿真环境特征分析、声速剖面分布、传播损失可视化
                  </Paragraph>
                </Card>
              </Col>
              
              <Col xs={24} md={8}>
                <Card size="small" hoverable>
                  <Title level={4}>模型性能</Title>
                  <Paragraph>
                    机器学习模型训练效果、特征重要性、预测精度分析
                  </Paragraph>
                </Card>
              </Col>
              
              <Col xs={24} md={8}>
                <Card size="small" hoverable>
                  <Title level={4}>联邦学习</Title>
                  <Paragraph>
                    分布式训练进度、客户端贡献度、模型聚合效果监控
                  </Paragraph>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard 