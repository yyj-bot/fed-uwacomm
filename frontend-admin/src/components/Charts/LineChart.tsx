import React from 'react';
import { EChartsWrapper } from './EChartsWrapper';
import type { EChartsOption } from 'echarts';

interface LineChartProps {
  title: string;
  xData: string[] | number[];
  yData: { name: string; data: number[]; color?: string }[];
  height?: number;
  showDataZoom?: boolean;
  showGrid?: boolean;
  yAxisName?: string;
  xAxisName?: string;
}

export const LineChart: React.FC<LineChartProps> = ({
  title,
  xData,
  yData,
  height = 400,
  showDataZoom = true,
  showGrid = true,
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
      containLabel: true,
      show: showGrid,
      borderColor: '#444'
    },
    toolbox: {
      feature: {
        saveAsImage: {
          backgroundColor: '#1f1f1f'
        }
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
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
      }
    },
    yAxis: {
      type: 'value',
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
      splitLine: {
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
        handleIcon: 'M10.7,11.9v-1.3H9.3v1.3c-4.9,0.3-8.8,4.4-8.8,9.4c0,5,3.9,9.1,8.8,9.4v1.3h1.3v-1.3c4.9-0.3,8.8-4.4,8.8-9.4C19.5,16.3,15.6,12.2,10.7,11.9z',
        handleSize: '80%',
        handleStyle: {
          color: '#fff'
        }
      }
    ] : undefined,
    series: yData.map(item => ({
      name: item.name,
      type: 'line',
      data: item.data,
      smooth: true,
      lineStyle: {
        color: item.color || undefined,
        width: 2
      },
      itemStyle: {
        color: item.color || undefined
      },
      emphasis: {
        focus: 'series'
      }
    }))
  };

  return <EChartsWrapper option={option} height={height} />;
}; 