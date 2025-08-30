import React, { useEffect, useRef } from 'react';
import * as echarts from 'echarts';

interface EChartsWrapperProps {
  option: echarts.EChartsOption;
  height?: number;
  theme?: string;
  className?: string;
  onChartReady?: (chart: echarts.ECharts) => void;
}

export const EChartsWrapper: React.FC<EChartsWrapperProps> = ({
  option,
  height = 400,
  theme = 'dark',
  className = '',
  onChartReady
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (chartRef.current) {
      // 初始化图表
      chartInstance.current = echarts.init(chartRef.current, theme);
      
      if (onChartReady) {
        onChartReady(chartInstance.current);
      }

      // 监听窗口大小变化
      const handleResize = () => {
        chartInstance.current?.resize();
      };
      
      window.addEventListener('resize', handleResize);
      
      return () => {
        window.removeEventListener('resize', handleResize);
        chartInstance.current?.dispose();
      };
    }
  }, [theme, onChartReady]);

  useEffect(() => {
    if (chartInstance.current && option) {
      chartInstance.current.setOption(option, true);
    }
  }, [option]);

  return (
    <div
      ref={chartRef}
      className={className}
      style={{ width: '100%', height: `${height}px` }}
    />
  );
}; 