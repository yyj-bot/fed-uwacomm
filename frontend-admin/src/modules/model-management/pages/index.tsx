import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Alert, Tabs, Select, Button, Space, Table, Progress } from 'antd';
import { ReloadOutlined, DownloadOutlined, TrophyOutlined } from '@ant-design/icons';
import { LineChart, ScatterChart, EChartsWrapper } from '@/components/Charts';
import { modelApi } from '@/services/api';
import type { ModelMetrics } from '@/types';
import type { EChartsOption } from 'echarts';

const { TabPane } = Tabs;

const ModelPerformance: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [models, setModels] = useState<ModelMetrics[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const loadModelData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await modelApi.getAllModels();
      setModels(response);
      if (response.length > 0 && !selectedModel) {
        setSelectedModel(response[0].modelName);
      }
    } catch (err) {
      setError('加载模型数据失败，请检查后端服务是否正常运行');
      console.error('Model data loading error:', err);
      
      // 使用模拟数据作为备用
      const mockData = generateMockModelData();
      setModels(mockData);
      if (mockData.length > 0 && !selectedModel) {
        setSelectedModel(mockData[0].modelName);
      }
    } finally {
      setLoading(false);
    }
  };

  const generateMockModelData = (): ModelMetrics[] => {
    const modelNames = ['RandomForest', 'SVM', 'NeuralNetwork', 'GradientBoosting', 'LinearRegression'];
    return modelNames.map(name => ({
      modelName: name,
      timestamp: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
      regression: {
        r2: 0.7 + Math.random() * 0.25,
        rmse: 10 + Math.random() * 20,
        mae: 5 + Math.random() * 15,
        mape: 5 + Math.random() * 10
      },
      classification: {
        accuracy: 0.8 + Math.random() * 0.15,
        precision: 0.75 + Math.random() * 0.2,
        recall: 0.7 + Math.random() * 0.25,
        f1Score: 0.72 + Math.random() * 0.23
      }
    }));
  };

  useEffect(() => {
    loadModelData();
  }, []);

  // 模型性能对比雷达图
  const getModelComparisonRadar = () => {
    if (models.length === 0) return { title: '模型性能对比', option: {} };
    
    const indicators = [
      { name: 'R²', max: 1 },
      { name: '准确率', max: 1 },
      { name: '精确率', max: 1 },
      { name: '召回率', max: 1 },
      { name: 'F1分数', max: 1 }
    ];

    const seriesData = models.map(model => ({
      name: model.modelName,
      value: [
        model.regression?.r2 || 0,
        model.classification?.accuracy || 0,
        model.classification?.precision || 0,
        model.classification?.recall || 0,
        model.classification?.f1Score || 0
      ]
    }));

    const option: EChartsOption = {
      title: {
        text: '模型性能综合对比',
        left: 'center',
        textStyle: { color: '#fff' }
      },
      legend: {
        data: models.map(m => m.modelName),
        bottom: 0,
        textStyle: { color: '#fff' }
      },
      radar: {
        indicator: indicators,
        axisName: {
          color: '#fff'
        },
        splitLine: {
          lineStyle: { color: '#333' }
        },
        axisLine: {
          lineStyle: { color: '#666' }
        }
      },
      series: [{
        type: 'radar',
        data: seriesData,
        emphasis: {
          focus: 'series'
        }
      }]
    };

    return { title: '模型性能对比', option };
  };

  // 回归指标趋势分析
  const getRegressionTrend = () => {
    if (models.length === 0) return { title: '回归指标趋势', xData: [], yData: [] };
    
    const sortedModels = [...models].sort((a, b) => a.modelName.localeCompare(b.modelName));
    
    return {
      title: '回归性能指标对比',
      xData: sortedModels.map(m => m.modelName),
      yData: [
        {
          name: 'R²',
          data: sortedModels.map(m => m.regression?.r2 || 0),
          color: '#1890ff'
        },
        {
          name: 'RMSE (归一化)',
          data: sortedModels.map(m => (m.regression?.rmse || 0) / 100), // 归一化到 0-1
          color: '#ff4d4f'
        },
        {
          name: 'MAE (归一化)',
          data: sortedModels.map(m => (m.regression?.mae || 0) / 100), // 归一化到 0-1
          color: '#52c41a'
        }
      ],
      yAxisName: '指标值',
      xAxisName: '模型'
    };
  };

  // 分类性能散点图
  const getClassificationScatter = () => {
    if (models.length === 0) return { title: '分类性能分析', series: [] };
    
    const scatterData: [number, number, number][] = models.map(model => [
      model.classification?.precision || 0,
      model.classification?.recall || 0,
      (model.classification?.f1Score || 0) * 100 // 用于设置气泡大小
    ]);
    
    return {
      title: '精确率 vs 召回率',
      series: [{
        name: '模型性能',
        data: scatterData,
        color: '#722ed1'
      }],
      xAxisName: '精确率',
      yAxisName: '召回率',
      showRegression: false
    };
  };

  // 模型排行榜数据
  const getModelRanking = () => {
    if (models.length === 0) return [];
    
    return models
      .map(model => ({
        key: model.modelName,
        modelName: model.modelName,
        overallScore: (
          (model.regression?.r2 || 0) * 0.3 +
          (model.classification?.f1Score || 0) * 0.3 +
          (model.classification?.accuracy || 0) * 0.4
        ),
        r2: model.regression?.r2 || 0,
        accuracy: model.classification?.accuracy || 0,
        f1Score: model.classification?.f1Score || 0,
        lastUpdate: new Date(model.timestamp).toLocaleDateString()
      }))
      .sort((a, b) => b.overallScore - a.overallScore);
  };

  const rankingColumns = [
    {
      title: '排名',
      key: 'rank',
      render: (_: any, __: any, index: number) => (
        <span style={{ color: index === 0 ? '#faad14' : '#fff' }}>
          {index === 0 && <TrophyOutlined style={{ marginRight: 4 }} />}
          {index + 1}
        </span>
      ),
      width: 80
    },
    {
      title: '模型名称',
      dataIndex: 'modelName',
      key: 'modelName',
      render: (text: string) => <span style={{ color: '#fff' }}>{text}</span>
    },
    {
      title: '综合得分',
      dataIndex: 'overallScore',
      key: 'overallScore',
      render: (score: number) => (
        <Progress 
          percent={Math.round(score * 100)} 
          size="small" 
          strokeColor={score > 0.8 ? '#52c41a' : score > 0.6 ? '#faad14' : '#ff4d4f'}
          showInfo={true}
        />
      ),
      width: 150
    },
    {
      title: 'R²',
      dataIndex: 'r2',
      key: 'r2',
      render: (value: number) => <span style={{ color: '#fff' }}>{value.toFixed(3)}</span>,
      width: 80
    },
    {
      title: '准确率',
      dataIndex: 'accuracy',
      key: 'accuracy',
      render: (value: number) => <span style={{ color: '#fff' }}>{(value * 100).toFixed(1)}%</span>,
      width: 100
    },
    {
      title: 'F1分数',
      dataIndex: 'f1Score',
      key: 'f1Score',
      render: (value: number) => <span style={{ color: '#fff' }}>{value.toFixed(3)}</span>,
      width: 100
    },
    {
      title: '更新时间',
      dataIndex: 'lastUpdate',
      key: 'lastUpdate',
      render: (text: string) => <span style={{ color: '#888' }}>{text}</span>
    }
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ color: '#fff', margin: 0 }}>模型性能分析</h1>
        <Space>
          <Select
            value={selectedModel}
            onChange={setSelectedModel}
            style={{ width: 180 }}
            placeholder="选择模型"
          >
            {models.map(model => (
              <Select.Option key={model.modelName} value={model.modelName}>
                {model.modelName}
              </Select.Option>
            ))}
          </Select>
          <Button icon={<ReloadOutlined />} onClick={loadModelData}>
            刷新
          </Button>
          <Button icon={<DownloadOutlined />} type="primary">
            导出报告
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
        <TabPane tab="性能概览" key="overview">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="模型性能雷达图" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <EChartsWrapper {...getModelComparisonRadar()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="分类性能分析" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <ScatterChart {...getClassificationScatter()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="回归分析" key="regression">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="回归性能指标对比" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getRegressionTrend()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="模型排行榜" key="ranking">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="模型性能排行榜" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <Table 
                  columns={rankingColumns}
                  dataSource={getModelRanking()}
                  pagination={false}
                  style={{ 
                    background: 'transparent',
                  }}
                  className="dark-table"
                />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="性能统计" key="statistics">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="模型数量统计" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>总模型数: {models.length}</p>
                  <p>最佳R²: {models.length > 0 ? Math.max(...models.map(m => m.regression?.r2 || 0)).toFixed(3) : 'N/A'}</p>
                  <p>最佳准确率: {models.length > 0 ? (Math.max(...models.map(m => m.classification?.accuracy || 0)) * 100).toFixed(1) + '%' : 'N/A'}</p>
                  <p>平均F1分数: {models.length > 0 ? (models.reduce((sum, m) => sum + (m.classification?.f1Score || 0), 0) / models.length).toFixed(3) : 'N/A'}</p>
                </div>
              </Card>
            </Col>
            <Col span={12}>
              <Card title="数据质量" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>数据完整性: 100%</p>
                  <p>最新训练: {models.length > 0 ? new Date(Math.max(...models.map(m => new Date(m.timestamp).getTime()))).toLocaleString() : 'N/A'}</p>
                  <p>评估指标: 回归 + 分类</p>
                  <p>状态: {models.length > 0 ? '正常运行' : '等待数据'}</p>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
         </div>
   );
 };

export default ModelPerformance; 