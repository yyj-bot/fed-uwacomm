import React from 'react';
import { EChartsWrapper } from './EChartsWrapper';
import type { EChartsOption } from 'echarts';

interface HeatmapChartProps {
  title: string;
  data: [number, number, number][]; // [x, y, value]
  xAxisData: string[] | number[];
  yAxisData: string[] | number[];
  height?: number;
  colorMap?: string[][];
  unit?: string;
  xAxisName?: string;
  yAxisName?: string;
}

export const HeatmapChart: React.FC<HeatmapChartProps> = ({
  title,
  data,
  xAxisData,
  yAxisData,
  height = 500,
  colorMap,
  unit = '',
  xAxisName = '',
  yAxisName = ''
}) => {
  // 计算数值范围用于颜色映射
  const values = data.map(item => item[2]);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);

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
      position: 'top',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: '#777',
      textStyle: {
        color: '#fff'
      },
      formatter: (params: any) => {
        const [x, y, value] = params.data;
        return `${xAxisName}: ${xAxisData[x]}<br/>${yAxisName}: ${yAxisData[y]}<br/>值: ${value.toFixed(2)} ${unit}`;
      }
    },
    grid: {
      height: '60%',
      top: '15%',
      left: '10%',
      right: '15%'
    },
    xAxis: {
      type: 'category',
      data: xAxisData,
      splitArea: {
        show: true
      },
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
      }
    },
    yAxis: {
      type: 'category',
      data: yAxisData,
      splitArea: {
        show: true
      },
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
      }
    },
    visualMap: {
      min: minValue,
      max: maxValue,
      calculable: true,
      orient: 'vertical',
      left: 'right',
      top: 'middle',
      textStyle: {
        color: '#fff'
      },
      inRange: {
        color: colorMap || [
          '#313695', '#4575b4', '#74add1', '#abd9e9', 
          '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', 
          '#f46d43', '#d73027', '#a50026'
        ]
      },
      formatter: `{value} ${unit}`
    },
    series: [{
      name: title,
      type: 'heatmap',
      data: data,
      label: {
        show: false
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  };

  return <EChartsWrapper option={option} height={height} />;
}; 