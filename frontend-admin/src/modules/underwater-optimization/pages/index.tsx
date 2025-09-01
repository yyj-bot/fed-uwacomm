import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Spin, Alert, Tabs, Select, Slider, Button, Space, Switch, InputNumber } from 'antd';
import { ReloadOutlined, DownloadOutlined, SettingOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { LineChart, ScatterChart, HeatmapChart, EChartsWrapper } from '@/components/Charts';
import { ofdmApi } from '@/services/api';
import type { OFDMPerformance } from '@/types';
import type { EChartsOption } from 'echarts';

const { TabPane } = Tabs;

interface OFDMConfig {
  subcarriers: number;
  cyclicPrefix: number;
  modulation: 'QPSK' | '16QAM' | '64QAM';
  codingRate: number;
  snrRange: [number, number];
  frequency: number;
}

const UnderwaterOptimization: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [ofdmData, setOfdmData] = useState<OFDMPerformance | null>(null);
  const [berData, setBerData] = useState<{ snr: number[]; ber: number[] } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [optimizationRunning, setOptimizationRunning] = useState(false);
  
  // OFDM 配置参数
  const [config, setConfig] = useState<OFDMConfig>({
    subcarriers: 64,
    cyclicPrefix: 16,
    modulation: 'QPSK',
    codingRate: 0.5,
    snrRange: [0, 30],
    frequency: 2000
  });

  const loadOFDMData = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const [performance, ber] = await Promise.all([
        ofdmApi.getOFDMPerformance(),
        ofdmApi.getBERCurve()
      ]);
      
      setOfdmData(performance);
      setBerData(ber);
    } catch (err) {
      setError('加载OFDM数据失败，请检查后端服务是否正常运行');
      console.error('OFDM data loading error:', err);
      
      // 使用模拟数据作为备用
      const mockOFDM = generateMockOFDMData();
      const mockBER = generateMockBERData();
      setOfdmData(mockOFDM);
      setBerData(mockBER);
    } finally {
      setLoading(false);
    }
  };

  const generateMockOFDMData = (): OFDMPerformance => {
    return {
      snrRange: Array.from({ length: 31 }, (_, i) => i),
      berResults: Array.from({ length: 31 }, (_, i) => Math.pow(10, -0.3 * i - 1 + Math.random() * 0.5)),
      constellation: {
        real: Array.from({ length: 100 }, () => (Math.random() - 0.5) * 2),
        imag: Array.from({ length: 100 }, () => (Math.random() - 0.5) * 2)
      },
      frequency: {
        spectrum: Array.from({ length: 512 }, (_, i) => Math.random() * Math.exp(-Math.abs(i - 256) / 50)),
        frequencies: Array.from({ length: 512 }, (_, i) => (i - 256) * 10)
      }
    };
  };

  const generateMockBERData = () => {
    const snr = Array.from({ length: 31 }, (_, i) => i);
    const ber = snr.map(s => Math.pow(10, -0.3 * s - 1 + Math.random() * 0.2));
    return { snr, ber };
  };

  useEffect(() => {
    loadOFDMData();
  }, []);

  // BER性能曲线
  const getBERCurve = () => {
    if (!berData) return { title: 'BER性能曲线', xData: [], yData: [] };
    
    return {
      title: 'OFDM系统BER性能曲线',
      xData: berData.snr.map(s => s.toString()),
      yData: [
        {
          name: 'BER',
          data: berData.ber.map(b => Math.log10(b)),
          color: '#ff4d4f'
        }
      ],
      xAxisName: 'SNR (dB)',
      yAxisName: 'log₁₀(BER)'
    };
  };

  // 星座图
  const getConstellationDiagram = () => {
    if (!ofdmData) return { title: '星座图', series: [] };
    
    const constellationData: [number, number][] = ofdmData.constellation.real.map((real, index) => [
      real, 
      ofdmData.constellation.imag[index]
    ]);
    
    return {
      title: `${config.modulation} 星座图`,
      series: [{
        name: '星座点',
        data: constellationData,
        color: '#1890ff'
      }],
      xAxisName: '同相分量 (I)',
      yAxisName: '正交分量 (Q)'
    };
  };

  // 频谱分析
  const getSpectrumAnalysis = () => {
    if (!ofdmData) return { title: '频谱分析', xData: [], yData: [] };
    
    return {
      title: 'OFDM信号频谱',
      xData: ofdmData.frequency.frequencies.map(f => (f / 1000).toFixed(1)),
      yData: [
        {
          name: '功率谱密度',
          data: ofdmData.frequency.spectrum.map(s => 10 * Math.log10(s + 1e-10)),
          color: '#52c41a'
        }
      ],
      xAxisName: '频率 (kHz)',
      yAxisName: '功率 (dB)'
    };
  };

  // 信道特性热力图
  const getChannelCharacteristics = () => {
    if (!ofdmData) return { title: '信道特性', data: [], xAxisData: [], yAxisData: [] };
    
    // 生成模拟的信道冲激响应
    const delays = Array.from({ length: 20 }, (_, i) => i * 0.1); // 0-2ms
    const frequencies = Array.from({ length: 50 }, (_, i) => i * 100); // 0-5kHz
    const heatmapData: [number, number, number][] = [];
    
    delays.forEach((delay, delayIndex) => {
      frequencies.forEach((freq, freqIndex) => {
        const amplitude = Math.exp(-delay * 2) * Math.exp(-Math.abs(freq - 2000) / 1000);
        heatmapData.push([freqIndex, delayIndex, amplitude + Math.random() * 0.1]);
      });
    });

    return {
      title: '水声信道冲激响应',
      data: heatmapData,
      xAxisData: frequencies.map(f => `${(f/1000).toFixed(1)}k`),
      yAxisData: delays.map(d => `${d.toFixed(1)}ms`),
      xAxisName: '频率',
      yAxisName: '时延',
      unit: ''
    };
  };

  // 优化参数对比
  const getOptimizationComparison = () => {
    const configurations = [
      { name: '当前配置', ber: berData?.ber[15] || 1e-3, throughput: 2.5 },
      { name: '优化配置1', ber: 5e-4, throughput: 3.2 },
      { name: '优化配置2', ber: 8e-4, throughput: 2.8 },
      { name: '优化配置3', ber: 3e-4, throughput: 2.1 }
    ];

    const comparisonData: [number, number, number][] = configurations.map((cfg, index) => [
      Math.log10(cfg.ber), 
      cfg.throughput, 
      index + 1
    ]);

    return {
      title: '配置性能对比 (吞吐量 vs BER)',
      series: [{
        name: '配置方案',
        data: comparisonData,
        color: '#722ed1'
      }],
      xAxisName: 'log₁₀(BER)',
      yAxisName: '吞吐量 (Mbps)',
      showRegression: false
    };
  };

  // 运行优化算法
  const runOptimization = async () => {
    setOptimizationRunning(true);
    
    // 模拟优化过程
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // 更新配置（模拟优化结果）
    setConfig(prev => ({
      ...prev,
      subcarriers: Math.round(prev.subcarriers * (0.9 + Math.random() * 0.2)),
      cyclicPrefix: Math.round(prev.cyclicPrefix * (0.8 + Math.random() * 0.4)),
      codingRate: Math.round((prev.codingRate * (0.9 + Math.random() * 0.2)) * 100) / 100
    }));
    
    setOptimizationRunning(false);
    
    // 重新加载数据
    loadOFDMData();
  };

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
        <h1 style={{ color: '#fff', margin: 0 }}>水声通信优化</h1>
        <Space>
          <Button 
            icon={<PlayCircleOutlined />} 
            type="primary" 
            loading={optimizationRunning}
            onClick={runOptimization}
          >
            运行优化
          </Button>
          <Button icon={<SettingOutlined />}>
            参数设置
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadOFDMData}>
            刷新
          </Button>
          <Button icon={<DownloadOutlined />} type="primary">
            导出结果
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

      <Tabs defaultActiveKey="performance" size="large">
        <TabPane tab="性能分析" key="performance">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="BER性能曲线" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getBERCurve()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="星座图" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <ScatterChart {...getConstellationDiagram()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="频谱分析" key="spectrum">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="频谱分析" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <LineChart {...getSpectrumAnalysis()} height={400} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="信道特性" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <HeatmapChart {...getChannelCharacteristics()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="参数优化" key="optimization">
          <Row gutter={[16, 16]}>
            <Col span={8}>
              <Card title="OFDM参数配置" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ padding: '20px', color: '#fff' }}>
                  <div style={{ marginBottom: '20px' }}>
                    <label>子载波数量: {config.subcarriers}</label>
                    <Slider
                      min={32}
                      max={256}
                      step={16}
                      value={config.subcarriers}
                      onChange={(value) => setConfig(prev => ({ ...prev, subcarriers: value }))}
                    />
                  </div>
                  
                  <div style={{ marginBottom: '20px' }}>
                    <label>循环前缀长度: {config.cyclicPrefix}</label>
                    <Slider
                      min={8}
                      max={32}
                      step={4}
                      value={config.cyclicPrefix}
                      onChange={(value) => setConfig(prev => ({ ...prev, cyclicPrefix: value }))}
                    />
                  </div>
                  
                  <div style={{ marginBottom: '20px' }}>
                    <label>调制方式:</label>
                    <Select
                      value={config.modulation}
                      onChange={(value) => setConfig(prev => ({ ...prev, modulation: value }))}
                      style={{ width: '100%', marginTop: '8px' }}
                    >
                      <Select.Option value="QPSK">QPSK</Select.Option>
                      <Select.Option value="16QAM">16QAM</Select.Option>
                      <Select.Option value="64QAM">64QAM</Select.Option>
                    </Select>
                  </div>
                  
                  <div style={{ marginBottom: '20px' }}>
                    <label>编码率: {config.codingRate}</label>
                    <Slider
                      min={0.1}
                      max={1.0}
                      step={0.1}
                      value={config.codingRate}
                      onChange={(value) => setConfig(prev => ({ ...prev, codingRate: value }))}
                    />
                  </div>
                  
                  <div>
                    <label>载波频率 (Hz):</label>
                    <InputNumber
                      min={500}
                      max={10000}
                      value={config.frequency}
                      onChange={(value) => setConfig(prev => ({ ...prev, frequency: value || 2000 }))}
                      style={{ width: '100%', marginTop: '8px' }}
                    />
                  </div>
                </div>
              </Card>
            </Col>
            <Col span={16}>
              <Card title="配置性能对比" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <ScatterChart {...getOptimizationComparison()} height={400} />
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab="系统状态" key="status">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="当前配置" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>子载波数量: {config.subcarriers}</p>
                  <p>循环前缀: {config.cyclicPrefix}</p>
                  <p>调制方式: {config.modulation}</p>
                  <p>编码率: {config.codingRate}</p>
                  <p>载波频率: {config.frequency} Hz</p>
                  <p>信噪比范围: {config.snrRange[0]}-{config.snrRange[1]} dB</p>
                </div>
              </Card>
            </Col>
            <Col span={12}>
              <Card title="性能指标" style={{ background: '#1f1f1f', border: '1px solid #333' }}>
                <div style={{ color: '#fff', padding: '20px' }}>
                  <p>当前BER: {berData ? berData.ber[15]?.toExponential(2) : 'N/A'}</p>
                  <p>理论吞吐量: {((config.subcarriers * Math.log2(config.modulation === 'QPSK' ? 4 : config.modulation === '16QAM' ? 16 : 64) * config.codingRate) / 1000).toFixed(2)} kbps</p>
                  <p>频谱效率: {(Math.log2(config.modulation === 'QPSK' ? 4 : config.modulation === '16QAM' ? 16 : 64) * config.codingRate).toFixed(2)} bps/Hz</p>
                  <p>优化状态: {optimizationRunning ? '运行中' : '完成'}</p>
                  <p>系统状态: 正常运行</p>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default UnderwaterOptimization; 