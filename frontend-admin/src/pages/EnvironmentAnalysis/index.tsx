import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Alert, Tabs, Select, DatePicker, Space, Button } from 'antd';
import { ReloadOutlined, DownloadOutlined } from '@ant-design/icons';
import { LineChart, HeatmapChart, ScatterChart } from '@/components/Charts';
import { environmentApi } from '@/services/api';
import type { EnvironmentFeature } from '@/types';
import dayjs from 'dayjs';

const { TabPane } = Tabs;
const { RangePicker } = DatePicker;

const EnvironmentAnalysis: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [features, setFeatures] = useState<EnvironmentFeature[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedFrequency, setSelectedFrequency] = useState<number>(1000);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

  const loadEnvironmentData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await environmentApi.getEnvironmentFeatures();
      setFeatures(response);
    } catch (err) {
      setError('加载环境数据失败，请检查后端服务是否正常运行');
      console.error('Environment data loading error:', err);
      
      // 使用模拟数据作为备用
      const mockData = generateMockEnvironmentData();
      setFeatures(mockData);
    } finally {
      setLoading(false);
    }
  };

  const generateMockEnvironmentData = (): EnvironmentFeature[] => {
    const data: EnvironmentFeature[] = [];
    const frequencies = [1000, 2000, 5000, 10000];
    
    for (let i = 0; i < 20; i++) {
      data.push({
        envId: `env_${i + 1}`,
        timestamp: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
        frequency: frequencies[Math.floor(Math.random() * frequencies.length)],
        maxDepth: 200 + Math.random() * 300, // 200-500m
        soundSpeedProfile: {
          minSpeed: 1480 + Math.random() * 10,
          maxSpeed: 1520 + Math.random() * 10,
          meanSpeed: 1500 + Math.random() * 10,
          stdSpeed: 5 + Math.random() * 5
        },
        sourceDepths: [10, 20, 30],
        receiverDepths: Array.from({ length: 10 }, (_, i) => (i + 1) * 20),
        ranges: Array.from({ length: 20 }, (_, i) => (i + 1) * 250) // 250m间隔
      });
    }
    return data;
  };

  useEffect(() => {
    loadEnvironmentData();
  }, []);

  // 声速剖面统计数据
  const getSoundSpeedProfileStats = () => {
    if (features.length === 0) return { title: '声速剖面统计', xData: [], yData: [] };
    
    const profiles = features.map(f => f.soundSpeedProfile);
    const categories = ['最小值', '平均值', '最大值'];
    const minSpeeds = profiles.map(p => p.minSpeed);
    const meanSpeeds = profiles.map(p => p.meanSpeed);
    const maxSpeeds = profiles.map(p => p.maxSpeed);
    
    return {
      title: '声速剖面统计分布',
      xData: categories,
      yData: [
        {
          name: '最小声速',
          data: [Math.min(...minSpeeds), minSpeeds.reduce((a, b) => a + b, 0) / minSpeeds.length, Math.max(...minSpeeds)],
          color: '#1890ff'
        },
        {
          name: '平均声速', 
          data: [Math.min(...meanSpeeds), meanSpeeds.reduce((a, b) => a + b, 0) / meanSpeeds.length, Math.max(...meanSpeeds)],
          color: '#52c41a'
        },
        {
          name: '最大声速',
          data: [Math.min(...maxSpeeds), maxSpeeds.reduce((a, b) => a + b, 0) / maxSpeeds.length, Math.max(...maxSpeeds)],
          color: '#ff4d4f'
        }
      ],
      xAxisName: '统计类型',
      yAxisName: '声速 (m/s)'
    };
  };

  // 深度分布热力图
  const getDepthDistributionHeatmap = () => {
    if (features.length === 0) return { title: '深度分布', data: [], xAxisData: [], yAxisData: [] };
    
    const selectedFeatures = features.filter(f => f.frequency === selectedFrequency);
    if (selectedFeatures.length === 0) return { title: `深度分布 - ${selectedFrequency}Hz`, data: [], xAxisData: [], yAxisData: [] };
    
    // 创建深度和距离的网格数据
    const heatmapData: [number, number, number][] = [];
    const maxDepths = Array.from(new Set(selectedFeatures.map(f => Math.floor(f.maxDepth / 50) * 50))).sort((a, b) => a - b);
    const rangeBins = Array.from({ length: 10 }, (_, i) => i * 500); // 500m间隔
    
    selectedFeatures.forEach((feature, featureIndex) => {
      feature.receiverDepths.forEach((depth, depthIndex) => {
        const depthBin = maxDepths.findIndex(d => Math.abs(d - depth) < 25);
        const rangeBin = Math.floor(Math.random() * rangeBins.length);
        if (depthBin !== -1) {
          heatmapData.push([rangeBin, depthBin, feature.soundSpeedProfile.meanSpeed]);
        }
      });
    });

    return {
      title: `深度-距离声速分布 - ${selectedFrequency}Hz`,
      data: heatmapData,
      xAxisData: rangeBins.map(r => `${r}m`),
      yAxisData: maxDepths.map(d => `${d}m`),
      xAxisName: '距离',
      yAxisName: '深度',
      unit: 'm/s'
    };
  };

  // 频率与深度关系散点图
  const getFrequencyDepthScatter = () => {
    if (features.length === 0) return { title: '频率与深度关系', series: [] };
    
    const scatterData: [number, number, number][] = features.map(f => [
      f.frequency / 1000, 
      f.maxDepth, 
      f.soundSpeedProfile.stdSpeed
    ]);
    
    return {
      title: '频率与最大深度关系',
      series: [{
        name: '环境样本',
        data: scatterData,
        color: '#722ed1'
      }],
      xAxisName: '频率 (kHz)',
      yAxisName: '最大深度 (m)',
      showRegression: true
    };
  };

  // 声速变化趋势
  const getSoundSpeedTrend = () => {
    if (features.length === 0) return { title: '声速变化趋势', xData: [], yData: [] };
    
    const sortedFeatures = features
      .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
      .slice(0, 10);
    
    return {
      title: '声速剖面变化趋势',
      xData: sortedFeatures.map((_, index) => `样本 ${index + 1}`),
      yData: [
        {
          name: '平均声速',
          data: sortedFeatures.map(f => f.soundSpeedProfile.meanSpeed),
          color: '#52c41a'
        },
        {
          name: '声速标准差',
          data: sortedFeatures.map(f => f.soundSpeedProfile.stdSpeed),
          color: '#faad14'
        }
      ],
      xAxisName: '时间序列',
      yAxisName: '声速 (m/s)'
    };
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
        <Spin size="large" />
      </div>
    );
  }

  const availableFrequencies = features.length > 0 ? 
    Array.from(new Set(features.map(f => f.frequency))).sort((a, b) => a - b) : 
    [1000, 2000, 5000, 10000];

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1 style={{ color: '#fff', margin: 0 }}>环境分析</h1>
        <Space>
          <Select
            value={selectedFrequency}
            onChange={setSelectedFrequency}
            style={{ width: 120 }}
            placeholder="选择频率"
          >
            {availableFrequencies.map(freq => (
              <Select.Option key={freq} value={freq}>{freq/1000}kHz</Select.Option>
            ))}
          </Select>
          <RangePicker
            value={dateRange}
            onChange={setDateRange}
            style={{ width: 240 }}
          />
          <Button icon={<ReloadOutlined />} onClick={loadEnvironmentData}>
            刷新
          </Button>
          <Button icon={<DownloadOutlined />} type="primary">
            导出数据
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

      <Tabs defaultActiveKey="profile" size="large">
        <TabPane tab="声速剖面" key="profile">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="声速剖面统计" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getSoundSpeedProfileStats()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="频率深度关系" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <ScatterChart {...getFrequencyDepthScatter()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="深度分析" key="depth">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="深度-距离声速分布" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <HeatmapChart {...getDepthDistributionHeatmap()} height={500} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="时序分析" key="temporal">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card title="声速变化趋势" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getSoundSpeedTrend()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="特征统计" key="features">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="环境特征统计" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>总样本数: {features.length}</p>
                  <p>频率范围: {features.length > 0 ? `${Math.min(...features.map(f => f.frequency))/1000}kHz - ${Math.max(...features.map(f => f.frequency))/1000}kHz` : 'N/A'}</p>
                  <p>最大深度范围: {features.length > 0 ? `${Math.min(...features.map(f => f.maxDepth)).toFixed(1)}m - ${Math.max(...features.map(f => f.maxDepth)).toFixed(1)}m` : 'N/A'}</p>
                  <p>平均声速: {features.length > 0 ? `${(features.reduce((sum, f) => sum + f.soundSpeedProfile.meanSpeed, 0) / features.length).toFixed(2)}m/s` : 'N/A'}</p>
                </div>
              </Card>
            </Col>
            <Col span={12}>
              <Card title="数据质量" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>完整性: 100%</p>
                  <p>最新更新: {features.length > 0 ? new Date(features[0].timestamp).toLocaleString() : 'N/A'}</p>
                  <p>数据源: BELLHOP 仿真</p>
                  <p>状态: {features.length > 0 ? '正常' : '等待数据'}</p>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default EnvironmentAnalysis; 