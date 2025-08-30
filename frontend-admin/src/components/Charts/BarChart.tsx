import React from 'react';
import { EChartsWrapper } from './EChartsWrapper';
import type { EChartsOption } from 'echarts';

interface BarChartProps {
  title: string;
  xData: string[];
  yData: { name: string; data: number[]; color?: string }[];
  height?: number;
  horizontal?: boolean;
  showDataZoom?: boolean;
  yAxisName?: string;
  xAxisName?: string;
}

export const BarChart: React.FC<BarChartProps> = ({
  title,
  xData,
  yData,
  height = 400,
  horizontal = false,
  showDataZoom = false,
  yAxisName,
  xAxisName
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
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#777',
      textStyle: {
        color: '#fff'
      }
    },
    legend: {
      top: '10%',
      textStyle: {
        color: '#fff'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: showDataZoom ? '15%' : '3%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: horizontal ? 'value' : 'category',
      data: horizontal ? undefined : xData,
      name: xAxisName,
      nameTextStyle: {
        color: '#fff'
      },
      axisLine: {
        lineStyle: {
          color: '#666'
        }
      },
      axisLabel: {
        color: '#fff'
      },
      splitLine: horizontal ? {
        lineStyle: {
          color: '#333'
        }
      } : undefined
    },
    yAxis: {
      type: horizontal ? 'category' : 'value',
      data: horizontal ? xData : undefined,
      name: yAxisName,
      nameTextStyle: {
        color: '#fff'
      },
      axisLine: {
        lineStyle: {
          color: '#666'
        }
      },
      axisLabel: {
        color: '#fff'
      },
      splitLine: horizontal ? undefined : {
        lineStyle: {
          color: '#333'
        }
      }
    },
    dataZoom: showDataZoom ? [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        start: 0,
        end: 100,
        handleStyle: {
          color: '#fff'
        }
      }
    ] : undefined,
    series: yData.map(item => ({
      name: item.name,
      type: 'bar',
      data: item.data,
      itemStyle: {
        color: item.color || undefined
      },
      emphasis: {
        focus: 'series'
      },
      barWidth: '60%'
    }))
  };

  return <EChartsWrapper option={option} height={height} />;
}; 