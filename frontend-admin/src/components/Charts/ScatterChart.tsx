import React from 'react';
import { EChartsWrapper } from './EChartsWrapper';
import type { EChartsOption } from 'echarts';

interface ScatterData {
  name: string;
  data: [number, number][] | [number, number, number][];
  color?: string;
  symbolSize?: number;
}

interface ScatterChartProps {
  title: string;
  series: ScatterData[];
  height?: number;
  xAxisName?: string;
  yAxisName?: string;
  showRegression?: boolean;
}

export const ScatterChart: React.FC<ScatterChartProps> = ({
  title,
  series,
  height = 400,
  xAxisName = '',
  yAxisName = '',
  showRegression = false
}) => {
  const option: EChartsOption = {
    title: {
      text: title,
      left: 'center',
      textStyle: {
        color: '#fff',
        fontSize: 16
      }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#777',
      textStyle: {
        color: '#fff'
      },
      formatter: (params: any) => {
        const { seriesName, data } = params;
        if (data.length === 3) {
          return `${seriesName}<br/>${xAxisName}: ${data[0]}<br/>${yAxisName}: ${data[1]}<br/>大小: ${data[2]}`;
        }
        return `${seriesName}<br/>${xAxisName}: ${data[0]}<br/>${yAxisName}: ${data[1]}`;
      }
    },
    legend: {
      top: '10%',
      textStyle: {
        color: '#fff'
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: xAxisName,
      nameLocation: 'middle',
      nameGap: 30,
      nameTextStyle: {
        color: '#fff'
      },
      axisLabel: {
        color: '#fff'
      },
      axisLine: {
        lineStyle: {
          color: '#666'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#333'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: yAxisName,
      nameLocation: 'middle',
      nameGap: 50,
      nameTextStyle: {
        color: '#fff'
      },
      axisLabel: {
        color: '#fff'
      },
      axisLine: {
        lineStyle: {
          color: '#666'
        }
      },
      splitLine: {
        lineStyle: {
          color: '#333'
        }
      }
    },
    series: series.map((item, index) => ({
      name: item.name,
      type: 'scatter',
      data: item.data,
      symbolSize: item.symbolSize || (item.data[0] && item.data[0].length === 3 ? 
        (params: number[]) => Math.sqrt(params[2]) * 5 : 8),
      itemStyle: {
        color: item.color || undefined,
        opacity: 0.8
      },
      emphasis: {
        focus: 'series',
        itemStyle: {
          opacity: 1
        }
      }
    }))
  };

  // 如果需要回归线，添加回归线数据
  if (showRegression && series.length > 0) {
    const firstSeries = series[0];
    const points = firstSeries.data as [number, number][];
    
    // 简单线性回归计算
    const n = points.length;
    const sumX = points.reduce((sum, [x]) => sum + x, 0);
    const sumY = points.reduce((sum, [, y]) => sum + y, 0);
    const sumXY = points.reduce((sum, [x, y]) => sum + x * y, 0);
    const sumXX = points.reduce((sum, [x]) => sum + x * x, 0);
    
    const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    const intercept = (sumY - slope * sumX) / n;
    
    const xMin = Math.min(...points.map(([x]) => x));
    const xMax = Math.max(...points.map(([x]) => x));
    
    const regressionLine = [
      [xMin, slope * xMin + intercept],
      [xMax, slope * xMax + intercept]
    ];

    (option.series as any[])?.push({
      name: '回归线',
      type: 'line',
      data: regressionLine,
      lineStyle: {
        color: '#ff6b6b',
        width: 2,
        type: 'dashed'
      },
      symbol: 'none',
      animation: false
    });
  }

  return <EChartsWrapper option={option} height={height} />;
}; 