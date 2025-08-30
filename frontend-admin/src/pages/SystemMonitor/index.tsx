import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Alert, Tabs, Button, Space, Table, Tag, Timeline, Statistic } from 'antd';
import { ReloadOutlined, WarningOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { LineChart, GaugeChart, BarChart, EChartsWrapper } from '@/components/Charts';
import { systemApi } from '@/services/api';
import type { EChartsOption } from 'echarts';

const { TabPane } = Tabs;
const { Countdown } = Statistic;

interface SystemHealth {
  status: 'healthy' | 'warning' | 'error';
  uptime: number;
  memory: number;
  cpu: number;
  timestamp: string;
}

interface LogEntry {
  id: string;
  timestamp: string;
  level: 'info' | 'warning' | 'error';
  module: string;
  message: string;
}

interface ServiceStatus {
  name: string;
  status: 'running' | 'stopped' | 'error';
  port: number;
  lastCheck: string;
  responseTime: number;
}

const SystemMonitor: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [systemHealth, setSystemHealth] = useState<SystemHealth[]>([]);
  const [currentHealth, setCurrentHealth] = useState<SystemHealth | null>(null);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [services, setServices] = useState<ServiceStatus[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const loadSystemData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const healthData = await systemApi.getSystemHealth();
      
      // 模拟实时更新健康数据
      const newHealthEntry: SystemHealth = {
        status: healthData.cpu > 80 || healthData.memory > 85 ? 'warning' : 'healthy',
        uptime: healthData.uptime,
        memory: healthData.memory,
        cpu: healthData.cpu,
        timestamp: new Date().toISOString()
      };
      
      setCurrentHealth(newHealthEntry);
      setSystemHealth(prev => [...prev.slice(-50), newHealthEntry]);
      
    } catch (err) {
      setError('加载系统数据失败，请检查后端服务是否正常运行');
      console.error('System data loading error:', err);
      
      // 使用模拟数据作为备用
      const mockHealth = generateMockHealthData();
      const mockLogs = generateMockLogs();
      const mockServices = generateMockServices();
      
      setCurrentHealth(mockHealth[mockHealth.length - 1]);
      setSystemHealth(mockHealth);
      setLogs(mockLogs);
      setServices(mockServices);
    } finally {
      setLoading(false);
    }
  };

  const generateMockHealthData = (): SystemHealth[] => {
    const data: SystemHealth[] = [];
    const now = Date.now();
    
    for (let i = 0; i < 50; i++) {
      const timestamp = new Date(now - (50 - i) * 30000); // 30秒间隔
      const cpu = 20 + Math.random() * 60 + Math.sin(i * 0.1) * 15;
      const memory = 40 + Math.random() * 40 + Math.sin(i * 0.15) * 10;
      
      data.push({
        status: cpu > 80 || memory > 85 ? 'warning' : cpu > 90 || memory > 95 ? 'error' : 'healthy',
        uptime: 86400 + i * 30, // 1天 + 增量
        memory: Math.max(0, Math.min(100, memory)),
        cpu: Math.max(0, Math.min(100, cpu)),
        timestamp: timestamp.toISOString()
      });
    }
    
    return data;
  };

  const generateMockLogs = (): LogEntry[] => {
    const modules = ['Backend', 'Frontend', 'Database', 'ML Engine', 'WebSocket'];
    const levels: LogEntry['level'][] = ['info', 'warning', 'error'];
    const messages = [
      '服务启动成功',
      '用户认证完成',
      '数据库连接建立',
      '模型训练开始',
      '内存使用率较高',
      '网络连接超时',
      '数据处理完成',
      '系统性能优化',
      '备份任务执行',
      '配置更新完成'
    ];
    
    const data: LogEntry[] = [];
    
    for (let i = 0; i < 20; i++) {
      data.push({
        id: `log_${i}`,
        timestamp: new Date(Date.now() - Math.random() * 3600000).toISOString(),
        level: levels[Math.floor(Math.random() * levels.length)],
        module: modules[Math.floor(Math.random() * modules.length)],
        message: messages[Math.floor(Math.random() * messages.length)]
      });
    }
    
    return data.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
  };

  const generateMockServices = (): ServiceStatus[] => {
    const serviceNames = ['Backend API', 'Frontend Server', 'Database', 'ML Service', 'WebSocket Server'];
    const statuses: ServiceStatus['status'][] = ['running', 'stopped', 'error'];
    
    return serviceNames.map((name, index) => ({
      name,
      status: index < 4 ? 'running' : statuses[Math.floor(Math.random() * statuses.length)],
      port: 8080 + index,
      lastCheck: new Date(Date.now() - Math.random() * 300000).toISOString(),
      responseTime: 50 + Math.random() * 200
    }));
  };

  useEffect(() => {
    loadSystemData();
    
    if (autoRefresh) {
      const interval = setInterval(loadSystemData, 30000); // 30秒刷新
      return () => clearInterval(interval);
    }
  }, [autoRefresh]);

  // 系统资源使用趋势
  const getResourceTrend = () => {
    if (systemHealth.length === 0) return { title: '资源使用趋势', xData: [], yData: [] };
    
    return {
      title: '系统资源使用趋势',
      xData: systemHealth.map((_, index) => `${index + 1}`),
      yData: [
        {
          name: 'CPU使用率',
          data: systemHealth.map(h => h.cpu),
          color: '#ff4d4f'
        },
        {
          name: '内存使用率',
          data: systemHealth.map(h => h.memory),
          color: '#1890ff'
        }
      ],
      showDataZoom: true,
      xAxisName: '时间点',
      yAxisName: '使用率 (%)'
    };
  };

  // 服务状态分布
  const getServiceStatusDistribution = () => {
    if (services.length === 0) return { title: '服务状态分布', option: {} };
    
    const statusCount = services.reduce((acc, service) => {
      acc[service.status] = (acc[service.status] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    const statusColors = {
      running: '#52c41a',
      stopped: '#faad14',
      error: '#ff4d4f'
    };

    const statusNames = {
      running: '运行中',
      stopped: '已停止',
      error: '错误'
    };

    const option: EChartsOption = {
      title: {
        text: '服务状态分布',
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
          name: '服务状态',
          type: 'pie',
          radius: ['40%', '70%'],
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

    return { title: '服务状态分布', option };
  };

  // 响应时间对比
  const getResponseTimeComparison = () => {
    if (services.length === 0) return { title: '服务响应时间', xData: [], yData: [] };
    
    const runningServices = services.filter(s => s.status === 'running');
    
    return {
      title: '服务响应时间对比',
      xData: runningServices.map(s => s.name),
      yData: [
        {
          name: '响应时间',
          data: runningServices.map(s => s.responseTime),
          color: '#722ed1'
        }
      ],
      horizontal: false,
      xAxisName: '服务',
      yAxisName: '响应时间 (ms)'
    };
  };

  // 日志表格列定义
  const logColumns = [
    {
      title: '时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (time: string) => (
        <span style={{ color: '#888' }}>
          {new Date(time).toLocaleTimeString()}
        </span>
      ),
      width: 100
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      render: (level: LogEntry['level']) => {
        const colors = {
          info: 'blue',
          warning: 'orange',
          error: 'red'
        };
        const texts = {
          info: '信息',
          warning: '警告',
          error: '错误'
        };
        return <Tag color={colors[level]}>{texts[level]}</Tag>;
      },
      width: 80
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      render: (text: string) => <span style={{ color: '#fff' }}>{text}</span>,
      width: 100
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      render: (text: string) => <span style={{ color: '#fff' }}>{text}</span>
    }
  ];

  // 服务表格列定义
  const serviceColumns = [
    {
      title: '服务名称',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <span style={{ color: '#fff' }}>{text}</span>
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: ServiceStatus['status']) => {
        const colors = {
          running: 'green',
          stopped: 'orange',
          error: 'red'
        };
        const texts = {
          running: '运行中',
          stopped: '已停止',
          error: '错误'
        };
        const icons = {
          running: <CheckCircleOutlined />,
          stopped: <WarningOutlined />,
          error: <CloseCircleOutlined />
        };
        return (
          <Tag color={colors[status]} icon={icons[status]}>
            {texts[status]}
          </Tag>
        );
      }
    },
    {
      title: '端口',
      dataIndex: 'port',
      key: 'port',
      render: (port: number) => <span style={{ color: '#fff' }}>{port}</span>
    },
    {
      title: '响应时间',
      dataIndex: 'responseTime',
      key: 'responseTime',
      render: (time: number) => <span style={{ color: '#fff' }}>{time.toFixed(0)}ms</span>
    },
    {
      title: '最后检查',
      dataIndex: 'lastCheck',
      key: 'lastCheck',
      render: (time: string) => (
        <span style={{ color: '#888' }}>
          {new Date(time).toLocaleTimeString()}
        </span>
      )
    }
  ];

  if (loading && systemHealth.length === 0) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ color: '#fff', margin: 0 }}>系统监控</h1>
        <Space>
          <Tag color={currentHealth?.status === 'healthy' ? 'green' : currentHealth?.status === 'warning' ? 'orange' : 'red'}>
            系统状态: {currentHealth?.status === 'healthy' ? '正常' : currentHealth?.status === 'warning' ? '警告' : '错误'}
          </Tag>
          <Button 
            type={autoRefresh ? 'primary' : 'default'}
            onClick={() => setAutoRefresh(!autoRefresh)}
          >
            {autoRefresh ? '停止自动刷新' : '开启自动刷新'}
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadSystemData}>
            手动刷新
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
        <TabPane tab="系统概览" key="overview">
          <Row gutter={[16, 16]} style={{ marginBottom: '16px' }}>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <GaugeChart 
                  title="CPU使用率" 
                  value={currentHealth ? Math.round(currentHealth.cpu) : 0}
                  height={200}
                  thresholds={[
                    { value: 60, color: '#52c41a' },
                    { value: 80, color: '#faad14' },
                    { value: 100, color: '#ff4d4f' }
                  ]}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <GaugeChart 
                  title="内存使用率" 
                  value={currentHealth ? Math.round(currentHealth.memory) : 0}
                  height={200}
                  thresholds={[
                    { value: 70, color: '#52c41a' },
                    { value: 85, color: '#faad14' },
                    { value: 100, color: '#ff4d4f' }
                  ]}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Statistic
                  title={<span style={{ color: '#fff' }}>系统运行时间</span>}
                  value={currentHealth ? Math.floor(currentHealth.uptime / 3600) : 0}
                  suffix={<span style={{ color: '#888' }}>小时</span>}
                  valueStyle={{ color: '#52c41a', fontSize: '24px' }}
                />
                <div style={{ marginTop: '20px', color: '#888' }}>
                  启动时间: {currentHealth ? new Date(Date.now() - currentHealth.uptime * 1000).toLocaleString() : 'N/A'}
                </div>
              </Card>
            </Col>
            <Col span={6}>
              <Card title="服务状态" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <EChartsWrapper {...getServiceStatusDistribution()} height={200} />
              </Card>
            </Col>
          </Row>
          
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="系统资源使用趋势" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getResourceTrend()} height={300} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="服务状态" key="services">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="服务响应时间" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <BarChart {...getResponseTimeComparison()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="服务详细信息" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Table
                  columns={serviceColumns}
                  dataSource={services}
                  rowKey="name"
                  pagination={false}
                  size="small"
                  style={{ background: 'transparent' }}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="系统日志" key="logs">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="最近日志" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Table
                  columns={logColumns}
                  dataSource={logs}
                  rowKey="id"
                  pagination={{ pageSize: 10 }}
                  size="small"
                  style={{ background: 'transparent' }}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="系统信息" key="info">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="系统统计" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>运行服务数: {services.filter(s => s.status === 'running').length}/{services.length}</p>
                  <p>平均CPU使用率: {systemHealth.length > 0 ? (systemHealth.reduce((sum, h) => sum + h.cpu, 0) / systemHealth.length).toFixed(1) + '%' : 'N/A'}</p>
                  <p>平均内存使用率: {systemHealth.length > 0 ? (systemHealth.reduce((sum, h) => sum + h.memory, 0) / systemHealth.length).toFixed(1) + '%' : 'N/A'}</p>
                  <p>错误日志数: {logs.filter(l => l.level === 'error').length}</p>
                  <p>警告日志数: {logs.filter(l => l.level === 'warning').length}</p>
                </div>
              </Card>
            </Col>
            <Col span={12}>
              <Card title="监控配置" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>自动刷新: {autoRefresh ? '开启' : '关闭'}</p>
                  <p>刷新间隔: 30秒</p>
                  <p>数据保留: 50个历史记录</p>
                  <p>监控模块: 全部</p>
                  <p>告警阈值: CPU{'>'} 80%, 内存{'>'} 85%</p>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default SystemMonitor; 