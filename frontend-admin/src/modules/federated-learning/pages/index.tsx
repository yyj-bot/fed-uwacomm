import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Alert, Tabs, Button, Space, Tag, Timeline, Progress, Table } from 'antd';
import { ReloadOutlined, PlayCircleOutlined, PauseCircleOutlined, StopOutlined } from '@ant-design/icons';
import { federatedApi } from '@/api';
import type { FederatedRound } from '@/types';
import type { EChartsOption } from 'echarts';

const { TabPane } = Tabs;

export interface ClientStatus {
  clientId: string;
  status: 'online' | 'offline' | 'training' | 'idle';
  lastUpdate: string;
  accuracy: number;
  loss: number;
  samples: number;
}

const FederatedLearning: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rounds, setRounds] = useState<FederatedRound[]>([]);
  const [clients, setClients] = useState<ClientStatus[]>([]);
  const [currentRound, setCurrentRound] = useState<number>(0);
  const [trainingStatus, setTrainingStatus] = useState<'running' | 'paused' | 'stopped'>('stopped');
  const [error, setError] = useState<string | null>(null);

  const loadFederatedData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      // 这里使用模拟的 sessionId
      const sessionId = 'session_001';
      const roundsData = await federatedApi.getFederatedRounds(sessionId);
      const clientsData = await federatedApi.getClientStatus();
      
      setRounds(roundsData);
      setClients(clientsData as ClientStatus[]);
      if (roundsData.length > 0) {
        setCurrentRound(roundsData[roundsData.length - 1].roundNumber);
      }
    } catch (err) {
      setError('加载联邦学习数据失败，请检查后端服务是否正常运行');
      console.error('Federated learning data loading error:', err);
      
      // 使用模拟数据作为备用
      const mockRounds = generateMockRoundsData();
      const mockClients = generateMockClientsData();
      setRounds(mockRounds);
      setClients(mockClients);
      if (mockRounds.length > 0) {
        setCurrentRound(mockRounds[mockRounds.length - 1].roundNumber);
      }
    } finally {
      setLoading(false);
    }
  };

  const generateMockRoundsData = (): FederatedRound[] => {
    const data: FederatedRound[] = [];
    for (let i = 1; i <= 20; i++) {
      data.push({
        roundNumber: i,
        timestamp: new Date(Date.now() - (20 - i) * 60000).toISOString(),
        globalMetrics: {
          globalLoss: Math.max(0.1, 2.0 - i * 0.08 + Math.random() * 0.1),
          globalAccuracy: Math.min(0.95, 0.3 + i * 0.03 + Math.random() * 0.05),
          r2: Math.min(0.9, 0.2 + i * 0.035 + Math.random() * 0.05)
        },
        clientCount: 5 + Math.floor(Math.random() * 3),
        clientMetrics: {
          avgLoss: Math.max(0.15, 2.2 - i * 0.09 + Math.random() * 0.15),
          stdLoss: 0.1 + Math.random() * 0.1,
          avgAccuracy: Math.min(0.92, 0.25 + i * 0.032 + Math.random() * 0.08),
          stdAccuracy: 0.02 + Math.random() * 0.03,
          totalSamples: 1000 + Math.floor(Math.random() * 500)
        }
      });
    }
    return data;
  };

  const generateMockClientsData = (): ClientStatus[] => {
    const statuses: ClientStatus['status'][] = ['online', 'training', 'idle', 'offline'];
    const data: ClientStatus[] = [];
    
    for (let i = 1; i <= 8; i++) {
      data.push({
        clientId: `client_${i.toString().padStart(3, '0')}`,
        status: statuses[Math.floor(Math.random() * statuses.length)],
        lastUpdate: new Date(Date.now() - Math.random() * 300000).toISOString(),
        accuracy: 0.7 + Math.random() * 0.25,
        loss: 0.1 + Math.random() * 0.5,
        samples: 50 + Math.floor(Math.random() * 200)
      });
    }
    return data;
  };

  useEffect(() => {
    loadFederatedData();
  }, []);

  // 训练进度图表
  const getTrainingProgress = () => {
    if (rounds.length === 0) return { title: '训练进度', xData: [], yData: [] };
    
    return {
      title: '联邦学习训练进度',
      xData: rounds.map(r => `轮次 ${r.roundNumber}`),
      yData: [
        {
          name: '全局损失',
          data: rounds.map(r => r.globalMetrics.globalLoss),
          color: '#ff4d4f'
        },
        {
          name: '全局准确率',
          data: rounds.map(r => r.globalMetrics.globalAccuracy),
          color: '#52c41a'
        },
        {
          name: 'R²',
          data: rounds.map(r => r.globalMetrics.r2),
          color: '#1890ff'
        }
      ],
      showDataZoom: true,
      xAxisName: '训练轮次',
      yAxisName: '指标值'
    };
  };

  // 客户端性能对比
  const getClientPerformance = () => {
    if (clients.length === 0) return { title: '客户端性能', xData: [], yData: [] };
    
    const onlineClients = clients.filter(c => c.status !== 'offline');
    
    return {
      title: '客户端性能对比',
      xData: onlineClients.map(c => c.clientId),
      yData: [
        {
          name: '准确率',
          data: onlineClients.map(c => c.accuracy),
          color: '#52c41a'
        },
        {
          name: '损失',
          data: onlineClients.map(c => c.loss),
          color: '#ff4d4f'
        }
      ],
      horizontal: false,
      xAxisName: '客户端',
      yAxisName: '指标值'
    };
  };

  // 客户端状态分布饼图
  const getClientStatusDistribution = () => {
    if (clients.length === 0) return { title: '客户端状态分布', option: {} };
    
    const statusCount = clients.reduce((acc, client) => {
      acc[client.status] = (acc[client.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    const statusColors = {
      online: '#52c41a',
      training: '#1890ff',
      idle: '#faad14',
      offline: '#ff4d4f'
    };

    const statusNames = {
      online: '在线',
      training: '训练中',
      idle: '空闲',
      offline: '离线'
    };

    const option: EChartsOption = {
      title: {
        text: '客户端状态分布',
        left: 'center',
        textStyle: { color: '#fff' }
      },
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: '#777',
        textStyle: { color: '#fff' },
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        textStyle: { color: '#fff' }
      },
      series: [
        {
          name: '客户端状态',
          type: 'pie',
          radius: '50%',
          data: Object.entries(statusCount).map(([status, count]) => ({
            value: count,
            name: statusNames[status as keyof typeof statusNames],
            itemStyle: {
              color: statusColors[status as keyof typeof statusColors]
            }
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    };

    return { title: '客户端状态分布', option };
  };

  // 客户端表格列定义
  const clientColumns = [
    {
      title: '客户端ID',
      dataIndex: 'clientId',
      key: 'clientId',
      render: (text: string) => <span style={{ color: '#fff' }}>{text}</span>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: ClientStatus['status']) => {
        const colors = {
          online: 'green',
          training: 'blue',
          idle: 'orange',
          offline: 'red'
        };
        const texts = {
          online: '在线',
          training: '训练中',
          idle: '空闲',
          offline: '离线'
        };
        return <Tag color={colors[status]}>{texts[status]}</Tag>;
      }
    },
    {
      title: '准确率',
      dataIndex: 'accuracy',
      key: 'accuracy',
      render: (value: number) => (
        <Progress 
          percent={Math.round(value * 100)} 
          size="small" 
          strokeColor={value > 0.8 ? '#52c41a' : value > 0.6 ? '#faad14' : '#ff4d4f'}
        />
      )
    },
    {
      title: '损失',
      dataIndex: 'loss',
      key: 'loss',
      render: (value: number) => <span style={{ color: '#fff' }}>{value.toFixed(3)}</span>
    },
    {
      title: '样本数',
      dataIndex: 'samples',
      key: 'samples',
      render: (value: number) => <span style={{ color: '#fff' }}>{value}</span>
    },
    {
      title: '最后更新',
      dataIndex: 'lastUpdate',
      key: 'lastUpdate',
      render: (time: string) => (
        <span style={{ color: '#888' }}>
          {new Date(time).toLocaleTimeString()}
        </span>
      )
    }
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <Spin size="large" />
      </div>
    );
  }

  const currentGlobalMetrics = rounds.length > 0 ? rounds[rounds.length - 1].globalMetrics : null;

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ color: '#fff', margin: 0 }}>联邦学习监控</h1>
        <Space>
          <Tag color={trainingStatus === 'running' ? 'green' : trainingStatus === 'paused' ? 'orange' : 'red'}>
            当前轮次: {currentRound}
          </Tag>
          <Button 
            icon={<PlayCircleOutlined />} 
            type={trainingStatus === 'running' ? 'default' : 'primary'}
            onClick={() => setTrainingStatus('running')}
          >
            开始训练
          </Button>
          <Button 
            icon={<PauseCircleOutlined />} 
            onClick={() => setTrainingStatus('paused')}
          >
            暂停
          </Button>
          <Button 
            icon={<StopOutlined />} 
            onClick={() => setTrainingStatus('stopped')}
          >
            停止
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadFederatedData}>
            刷新
          </Button>
        </Space>
      </div>

      {error && (
        <Alert
          message="数据加载提醒"
          description={error}
          type="warning"
          showIcon
          style={{ marginBottom: '16px' }}
        />
      )}

      <Tabs defaultActiveKey="overview" size="large">
        <TabPane tab="训练概览" key="overview">
          <Row gutter={[16, 16]} style={{ marginBottom: '16px' }}>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <GaugeChart 
                  title="全局准确率" 
                  value={currentGlobalMetrics ? Math.round(currentGlobalMetrics.globalAccuracy * 100) : 0}
                  height={200}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <GaugeChart 
                  title="R² 系数" 
                  value={currentGlobalMetrics ? Math.round(currentGlobalMetrics.r2 * 100) : 0}
                  height={200}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <GaugeChart 
                  title="活跃客户端" 
                  value={clients.filter(c => c.status !== 'offline').length}
                  max={clients.length}
                  unit=""
                  height={200}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card title="客户端状态" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <EChartsWrapper {...getClientStatusDistribution()} height={200} />
              </Card>
            </Col>
          </Row>
          
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="训练进度曲线" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getTrainingProgress()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="客户端管理" key="clients">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="客户端性能对比" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <BarChart {...getClientPerformance()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="客户端详细信息" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Table
                  columns={clientColumns}
                  dataSource={clients}
                  rowKey="clientId"
                  pagination={{ pageSize: 6 }}
                  size="small"
                  style={{ background: 'transparent' }}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="训练历史" key="history">
          <Row gutter={[16, 16]}>
            <Col span={16}>
              <Card title="训练轮次详情" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Timeline
                  mode="left"
                  style={{ maxHeight: '500px', overflowY: 'auto' }}
                >
                  {rounds.slice(-10).reverse().map(round => (
                    <Timeline.Item
                      key={round.roundNumber}
                      color={round.globalMetrics.globalAccuracy > 0.8 ? 'green' : 'blue'}
                    >
                      <div style={{ color: '#fff' }}>
                        <h4>轮次 {round.roundNumber}</h4>
                        <p>准确率: {(round.globalMetrics.globalAccuracy * 100).toFixed(1)}%</p>
                        <p>损失: {round.globalMetrics.globalLoss.toFixed(3)}</p>
                        <p>参与客户端: {round.clientCount}</p>
                        <p style={{ color: '#888', fontSize: '12px' }}>
                          {new Date(round.timestamp).toLocaleString()}
                        </p>
                      </div>
                    </Timeline.Item>
                  ))}
                </Timeline>
              </Card>
            </Col>
            <Col span={8}>
              <Card title="统计信息" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>总训练轮次: {rounds.length}</p>
                  <p>最佳准确率: {rounds.length > 0 ? (Math.max(...rounds.map(r => r.globalMetrics.globalAccuracy)) * 100).toFixed(1) + '%' : 'N/A'}</p>
                  <p>最低损失: {rounds.length > 0 ? Math.min(...rounds.map(r => r.globalMetrics.globalLoss)).toFixed(3) : 'N/A'}</p>
                  <p>平均客户端数: {rounds.length > 0 ? Math.round(rounds.reduce((sum, r) => sum + r.clientCount, 0) / rounds.length) : 'N/A'}</p>
                  <p>总样本数: {rounds.length > 0 ? rounds[rounds.length - 1].clientMetrics.totalSamples : 'N/A'}</p>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default FederatedLearning; 