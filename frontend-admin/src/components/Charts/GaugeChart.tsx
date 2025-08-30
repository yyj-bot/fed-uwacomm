import React from 'react';
import { EChartsWrapper } from './EChartsWrapper';
import type { EChartsOption } from 'echarts';

interface GaugeChartProps {
  title: string;
  value: number;
  max?: number;
  min?: number;
  unit?: string;
  height?: number;
  color?: string[];
  thresholds?: { value: number; color: string }[];
}

export const GaugeChart: React.FC<GaugeChartProps> = ({
  title,
  value,
  max = 100,
  min = 0,
  unit = '%',
  height = 400,
  color,
  thresholds
}) => {
  const option: EChartsOption = {
    title: {
      text: title,
      left: 'center',
      top: '5%',
      textStyle: {
        color: '#fff',
        fontSize: 16
      }
    },
    series: [
      {
        type: 'gauge',
        center: ['50%', '60%'],
        startAngle: 200,
        endAngle: -20,
        min: min,
        max: max,
        splitNumber: 10,
        itemStyle: {
          color: color?.[0] || '#58D9F9'
        },
        progress: {
          show: true,
          width: 30
        },
        pointer: {
          show: false
        },
        axisLine: {
          lineStyle: {
            width: 30,
            color: thresholds ? 
              thresholds.map(t => [t.value / max, t.color]) :
              [
                [0.3, '#FF6E76'],
                [0.7, '#FDDD60'],
                [1, '#58D9F9']
              ]
          }
        },
        axisTick: {
          distance: -45,
          splitNumber: 5,
          lineStyle: {
            width: 2,
            color: '#999'
          }
        },
        splitLine: {
          distance: -52,
          length: 14,
          lineStyle: {
            width: 3,
            color: '#999'
          }
        },
        axisLabel: {
          distance: -20,
          color: '#999',
          fontSize: 12
        },
        anchor: {
          show: false
        },
        title: {
          show: false
        },
        detail: {
          valueAnimation: true,
          width: '60%',
          lineHeight: 40,
          borderRadius: 8,
          offsetCenter: [0, '-15%'],
          fontSize: 30,
          fontWeight: 'bolder',
          formatter: `{value}${unit}`,
          color: 'inherit'
        },
        data: [
          {
            value: value,
            name: title
          }
        ]
      }
    ]
  };

  return <EChartsWrapper option={option} height={height} />;
}; 